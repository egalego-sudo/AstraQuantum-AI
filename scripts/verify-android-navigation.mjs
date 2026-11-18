import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";

const source = await readFile(
  new URL("../app/src/main/java/com/bittupandit/astraquantumai/MainActivity.kt", import.meta.url),
  "utf8"
);

const navigationStart = source.indexOf("Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())");
assert.ok(navigationStart >= 0, "Top-level horizontal navigation row must be present");
const navigationBlock = source.slice(navigationStart, navigationStart + 1500).match(/listOf\(([^)]*)\)\.forEach\s*\{\s*item\s*->/);
assert.ok(navigationBlock, "Top-level navigation list must be present");
const navigation = [...navigationBlock[1].matchAll(/"([^"]+)"/g)].map(match => match[1]);
assert.ok(navigation.length > 0, "Navigation list must not be empty");
assert.equal(new Set(navigation).size, navigation.length, "Navigation labels must be unique");

const routes = [...source.matchAll(/^ {12}"([^"]+)"\s*->/gm)].map(match => match[1]);
assert.ok(routes.length > 0, "Top-level page route cases must be present");
assert.equal(new Set(routes).size, routes.length, "Page route cases must be unique");

const missingRoutes = navigation.filter(label => !routes.includes(label));
assert.deepEqual(missingRoutes, [], "Every visible navigation item must have a route: " + missingRoutes.join(", "));

const unlinkedRoutes = routes.filter(label => !navigation.includes(label));
assert.deepEqual(unlinkedRoutes, [], "Every page route must be reachable from top-level navigation: " + unlinkedRoutes.join(", "));

assert.ok(source.includes("Creator by Bittu Pandit"), "Creator credit must remain visible");
assert.ok(source.includes("QUANTUM AI"), "Current product name must remain visible");

console.log("PASS Android navigation: " + navigation.length + " unique items, all routes reachable, creator credit and product branding present.");
