import { existsSync, mkdtempSync, readFileSync, readdirSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { execFileSync } from "node:child_process";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");

function parseArgs() {
  const args = process.argv.slice(2);
  const get = (flag) => {
    const i = args.indexOf(flag);
    return i >= 0 ? args[i + 1] : undefined;
  };
  return {
    gitops: get("--gitops"),
    registry: resolve(get("--registry") ?? join(ROOT, "docs", "architecture", "platform-registry.json")),
    strict: args.includes("--strict"),
  };
}

function loadRegistry(path) {
  const raw = readFileSync(path, "utf-8");
  const registry = JSON.parse(raw);
  if (registry.schemaVersion !== 1) {
    throw new Error(`platform-registry.json schemaVersion must be 1 (got ${registry.schemaVersion})`);
  }
  return registry;
}

function listDirs(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter((e) => e.isDirectory())
    .map((e) => e.name);
}

function listFiles(dir, ext) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((f) => f.endsWith(ext))
    .map((f) => f.replace(new RegExp(`\\${ext}$`), ""));
}

function cloneGitops(owner, name) {
  const tmp = mkdtempSync(join(tmpdir(), "aegis-gitops-"));
  execFileSync("git", ["clone", "--depth", "1", `https://github.com/${owner}/${name}.git`, join(tmp, name)], {
    stdio: "ignore",
  });
  return join(tmp, name);
}

function report(issues) {
  if (issues.length === 0) {
    console.log("OK: documentation aligned with platform-registry.json");
    return;
  }
  console.log(`DRIFT: ${issues.length} issue(s) found`);
  for (const issue of issues) console.log(`  - ${issue}`);
  process.exitCode = 1;
}

const opts = parseArgs();
const registry = loadRegistry(opts.registry);
const issues = [];

const backendRoot = join(ROOT, "backend");
const expectedModules = [
  ...registry.artifacts.backendServices.map((s) => s.module),
  ...registry.artifacts.sharedLibraries.map((s) => s.module),
];
const actualModules = listDirs(backendRoot).filter((d) => d.startsWith("aegis-") && d !== "target");

const missingModules = expectedModules.filter((m) => !actualModules.includes(m));
const unknownModules = actualModules.filter((m) => !expectedModules.includes(m));
for (const m of missingModules) issues.push(`backend module declared in registry but missing on disk: ${m}`);
for (const m of unknownModules) issues.push(`backend module on disk but not declared in registry: ${m}`);

let gitopsRoot = opts.gitops;
if (!gitopsRoot) {
  const repo = registry.repositories.gitops;
  gitopsRoot = cloneGitops(repo.owner, repo.name);
}

const chartDirs = listDirs(join(gitopsRoot, "charts"));
const expectedCharts = registry.artifacts.backendServices
  .filter((s) => s.gitops.chart)
  .map((s) => s.id)
  .concat(registry.artifacts.frontend.gitops.chart ? [registry.artifacts.frontend.id] : []);
const missingCharts = expectedCharts.filter((c) => !chartDirs.includes(c));
const unknownCharts = chartDirs.filter((c) => !expectedCharts.includes(c));
for (const c of missingCharts) issues.push(`Aegis-GitOps missing chart for service: ${c}`);
for (const c of unknownCharts) issues.push(`Aegis-GitOps has chart not declared in registry: ${c}`);

const devApps = listFiles(join(gitopsRoot, "applications", "dev"), ".yaml");
const expectedDevApps = registry.gitops.applicationsDev;
for (const a of expectedDevApps.filter((a) => !devApps.includes(a))) {
  issues.push(`Aegis-GitOps applications/dev missing: ${a}.yaml`);
}
for (const a of devApps.filter((a) => !expectedDevApps.includes(a))) {
  issues.push(`Aegis-GitOps applications/dev has undeclared app: ${a}`);
}

for (const env of ["pre", "stage", "prod"]) {
  const envApps = listFiles(join(gitopsRoot, "applications", env), ".yaml");
  const expectedEnvApps = registry.artifacts.backendServices
    .filter((s) => s.gitops.application[env])
    .map((s) => s.id)
    .concat(registry.artifacts.frontend.gitops.application[env] ? [registry.artifacts.frontend.id] : []);
  for (const a of expectedEnvApps.filter((a) => !envApps.includes(a))) {
    issues.push(`Aegis-GitOps applications/${env} missing: ${a}.yaml`);
  }
  for (const a of envApps.filter((a) => !expectedEnvApps.includes(a))) {
    issues.push(`Aegis-GitOps applications/${env} has undeclared app: ${a}`);
  }
}

const appOfAppsExists = existsSync(join(gitopsRoot, "applications", "app-of-apps-dev.yaml"));
if (registry.gitops.appOfApps.dev !== appOfAppsExists) {
  issues.push(`app-of-apps-dev.yaml present=${appOfAppsExists} but registry says dev appOfApps=${registry.gitops.appOfApps.dev}`);
}

const scanDocs = ["README.md", "docs/project-status.md", "docs/architecture/service-catalog.md"];
for (const phrase of registry.platform.forbiddenClaimPhrases) {
  for (const doc of scanDocs) {
    const p = join(ROOT, doc);
    if (!existsSync(p)) continue;
    const content = readFileSync(p, "utf-8");
    const idx = content.indexOf(phrase);
    if (idx !== -1) {
      issues.push(`forbidden claim "${phrase}" found in ${doc} (line ${content.slice(0, idx).split("\n").length})`);
    }
  }
}

if (!opts.gitops) rmSync(dirname(gitopsRoot), { recursive: true, force: true });

report(issues);
