import test from "node:test";
import assert from "node:assert/strict";
import { clientAddress } from "./client-address.js";

test("prefers Cloudflare's visitor IP over caller-controlled forwarded values", () => {
  const req = {
    headers: {
      "cf-connecting-ip": "203.0.113.25",
      "x-forwarded-for": "198.51.100.99, 203.0.113.8"
    },
    socket: { remoteAddress: "10.0.0.1" }
  };
  assert.equal(clientAddress(req), "203.0.113.25");
});

test("does not trust X-Forwarded-For when the Cloudflare header is missing", () => {
  const req = {
    headers: { "x-forwarded-for": "198.51.100.99, 203.0.113.8" },
    socket: { remoteAddress: "10.0.0.1" }
  };
  assert.equal(clientAddress(req), "10.0.0.1");
});

test("falls back to the socket address when the Cloudflare value is invalid", () => {
  const req = {
    headers: { "cf-connecting-ip": "not-an-ip" },
    socket: { remoteAddress: "127.0.0.1" }
  };
  assert.equal(clientAddress(req), "127.0.0.1");
});

test("supports valid IPv6 Cloudflare addresses", () => {
  const req = {
    headers: { "cf-connecting-ip": "2001:db8::7" },
    socket: { remoteAddress: "127.0.0.1" }
  };
  assert.equal(clientAddress(req), "2001:db8::7");
});

test("has a safe fallback for missing or invalid request address data", () => {
  assert.equal(clientAddress({ headers: {} }), "unknown");
  assert.equal(clientAddress({ headers: {}, socket: { remoteAddress: "bad" } }), "unknown");
});
