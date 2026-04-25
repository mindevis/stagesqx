#!/usr/bin/env node
/**
 * Sets mod_version= in gradle.properties (semver without leading v — NeoForge).
 */
import fs from "node:fs";
const raw = (process.argv[2] || "").replace(/^v/i, "").trim();
if (!raw) {
  console.error("usage: bump-gradle-mod-version.mjs <semver>");
  process.exit(1);
}
const path = "gradle.properties";
let t = fs.readFileSync(path, "utf8");
if (!/^mod_version=/m.test(t)) {
  console.error(`${path}: mod_version= not found`);
  process.exit(1);
}
t = t.replace(/^mod_version=.*/m, `mod_version=${raw}`);
fs.writeFileSync(path, t);
console.log("mod_version set to", raw);
