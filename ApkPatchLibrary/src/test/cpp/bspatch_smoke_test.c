#define _POSIX_C_SOURCE 200809L
#include "bspatch.h"

#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#define PATCH_LIMIT (80LL * 1024LL * 1024LL)
#define APK_LIMIT (150LL * 1024LL * 1024LL)

static void write_all(int fd, const unsigned char *data, size_t length) {
    size_t offset = 0;
    while (offset < length) {
        ssize_t n = write(fd, data + offset, length - offset);
        if (n <= 0) {
            perror("write");
            exit(2);
        }
        offset += (size_t)n;
    }
}

static void encode_offt(unsigned char out[8], uint64_t value) {
    for (int i = 0; i < 8; ++i) {
        out[i] = (unsigned char)(value & 0xffU);
        value >>= 8;
    }
}

static void make_header(unsigned char header[32], uint64_t ctrl,
                        uint64_t diff, uint64_t output) {
    memset(header, 0, 32);
    memcpy(header, "BSDIFF40", 8);
    encode_offt(header + 8, ctrl);
    encode_offt(header + 16, diff);
    encode_offt(header + 24, output);
}

static int make_file(const char *directory, const char *name,
                     const unsigned char *data, size_t length) {
    char path[512];
    if (snprintf(path, sizeof(path), "%s/%s", directory, name) >= (int)sizeof(path)) {
        fprintf(stderr, "temporary path too long\n");
        exit(2);
    }
    int fd = open(path, O_CREAT | O_TRUNC | O_RDWR, 0600);
    if (fd < 0) {
        perror("open");
        exit(2);
    }
    if (data && length) write_all(fd, data, length);
    return fd;
}

static void expect_result(const char *label, int actual, int expected) {
    if (actual != expected) {
        fprintf(stderr, "%s: expected %d, got %d\n", label, expected, actual);
        exit(1);
    }
    printf("PASS %s\n", label);
}

int main(void) {
    char template[] = "/tmp/quantum-bspatch-test-XXXXXX";
    char *directory = mkdtemp(template);
    if (!directory) {
        perror("mkdtemp");
        return 2;
    }

    char old_path[512], patch_path[512], output_path[512], missing_path[512];
    snprintf(old_path, sizeof(old_path), "%s/old.apk", directory);
    snprintf(patch_path, sizeof(patch_path), "%s/patch.bin", directory);
    snprintf(output_path, sizeof(output_path), "%s/new.apk", directory);
    snprintf(missing_path, sizeof(missing_path), "%s/missing.patch", directory);

    int old_fd = open(old_path, O_CREAT | O_TRUNC | O_WRONLY, 0600);
    if (old_fd < 0) { perror("old file"); return 2; }
    write_all(old_fd, (const unsigned char *)"old", 3);
    close(old_fd);

    expect_result("null arguments", bspatch(NULL, output_path, patch_path),
                  BSPATCH_ERR_INVALID_ARGUMENT);
    expect_result("missing patch", bspatch(old_path, output_path, missing_path),
                  BSPATCH_ERR_OPEN_PATCH);

    int fd = make_file(directory, "patch.bin", (const unsigned char *)"short", 5);
    close(fd);
    expect_result("truncated header", bspatch(old_path, output_path, patch_path),
                  BSPATCH_ERR_CORRUPT_PATCH);

    unsigned char header[32];
    make_header(header, 0, 0, 1);
    header[0] = 'X';
    fd = make_file(directory, "patch.bin", header, sizeof(header));
    close(fd);
    expect_result("invalid magic", bspatch(old_path, output_path, patch_path),
                  BSPATCH_ERR_CORRUPT_PATCH);

    make_header(header, 1000, 0, 1);
    fd = make_file(directory, "patch.bin", header, sizeof(header));
    close(fd);
    expect_result("control segment beyond file", bspatch(old_path, output_path, patch_path),
                  BSPATCH_ERR_CORRUPT_PATCH);

    make_header(header, 0, 0, (uint64_t)APK_LIMIT + 1U);
    fd = make_file(directory, "patch.bin", header, sizeof(header));
    close(fd);
    expect_result("output size above APK ceiling", bspatch(old_path, output_path, patch_path),
                  BSPATCH_ERR_CORRUPT_PATCH);

    fd = make_file(directory, "patch.bin", header, sizeof(header));
    if (ftruncate(fd, (off_t)PATCH_LIMIT + 1) != 0) {
        perror("ftruncate");
        close(fd);
        return 2;
    }
    close(fd);
    expect_result("patch size above ceiling", bspatch(old_path, output_path, patch_path),
                  BSPATCH_ERR_CORRUPT_PATCH);

    unlink(old_path);
    unlink(patch_path);
    unlink(output_path);
    rmdir(directory);
    puts("All malformed-patch smoke tests passed.");
    return 0;
}
