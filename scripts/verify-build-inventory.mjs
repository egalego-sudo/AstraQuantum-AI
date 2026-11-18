import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";

const inventoryPath = new URL("../docs/BUILD-INVENTORY-1-385.md", import.meta.url);
const markdown = await readFile(inventoryPath, "utf8");
const csvBlock = markdown.match(/```csv\n([\s\S]*?)\n```/);
assert.ok(csvBlock, "Build inventory must contain a fenced CSV block");

const rows = csvBlock[1].split(/\r?\n/).filter(Boolean).slice(1).map(line => {
  const match = line.match(/^"(\d+)","([^"]*)","([^"]*)","([^"]*)","([^"]*)","([^"]*)","([^"]*)","([^"]*)"/);
  assert.ok(match, "Malformed build inventory row: " + line.slice(0, 120));
  return {
    build: Number(match[1]),
    runId: match[2],
    status: match[3],
    commitSha: match[5],
    releaseTag: match[7],
    releaseAssets: match[8]
  };
});

assert.equal(rows.length, 385, "Inventory must include every build number from 1 through 385");
assert.deepEqual(rows.map(row => row.build), Array.from({ length: 385 }, (_, index) => index + 1));
assert.equal(new Set(rows.map(row => row.build)).size, 385, "Build numbers must be unique");

const androidRuns = rows.filter(row => row.runId !== "");
assert.equal(androidRuns.length, 380, "Expected 380 Android-build workflow runs");
assert.deepEqual(androidRuns.map(row => row.build).filter(build => build <= 149).length, 147);
assert.deepEqual(
  rows.filter(row => !row.runId).map(row => row.build),
  [91, 92, 203, 204, 207],
  "Missing Android build numbers must be explicitly accounted for"
);

const statusCounts = Object.fromEntries(["success", "failure", "cancelled"].map(status => [
  status, androidRuns.filter(row => row.status === status).length
]));
assert.deepEqual(statusCounts, { success: 185, failure: 61, cancelled: 134 });

const releases = rows.filter(row => row.releaseTag !== "");
assert.equal(releases.length, 36, "Expected 36 published build releases");
assert.ok(releases.every(row => Number(row.releaseAssets) > 0), "Every listed release must include asset-count metadata");

const sourceCommits = androidRuns.filter(row => row.commitSha);
const missingCommitTrees = sourceCommits.filter(row => {
  try {
    execFileSync("git", ["cat-file", "-e", row.commitSha + "^{tree}"], { stdio: "ignore" });
    return false;
  } catch {
    return true;
  }
});
assert.equal(missingCommitTrees.length, 0, "Every inventoried Android build commit must be present in the Git object database: " + missingCommitTrees.map(row => row.build).join(", "));

const earlyRuns = androidRuns.filter(row => row.build <= 149);
assert.equal(earlyRuns.filter(row => row.status === "success").length, 111);
assert.equal(earlyRuns.filter(row => row.status === "failure").length, 36);

console.log("PASS Build 1–385 inventory: 385 sequential rows, 380 Android runs, 5 accounted gaps, 36 releases, " + sourceCommits.length + " source commits present in Git.");
