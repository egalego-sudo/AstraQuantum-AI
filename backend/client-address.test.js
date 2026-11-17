import test from "node:test";
import assert from "node:assert/strict";
import { clientAddress } from "./client-address.js";

test("uses the right-most address in the forwarded proxy chain", () => {
  const req = {
    headers: { "x-forwarded-for": "198.51.100.99, 203.0.113.8, 192.0.2.44" },
    socket: { remoteAddress: "10.0.0.1" }
  };
  assert.equal(clientAddress(req), "192.0.2.44");
});

test("trims whitespace and ignores empty forwarded entries", () => {
  const req = {
    headers: { "x-forwarded-for": " 198.51.100.4, , 203.0.113.7 " },
    socket: { remoteAddress: "10.0.0.1" }
  };
  assert.equal(clientAddress(req), "203.0.113.7");
});

test("falls back to the socket address when no forwarded chain exists", () => {
  assert.equal(clientAddress({ headers: {}, socket: { remoteAddress: "127.0.0.1" } }), "127.0.0.1");
});

test("has a safe fallback for missing request address data", () => {
  assert.equal(clientAddress({ headers: {} }), "unknown");
});
