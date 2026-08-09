#!/usr/bin/env python3
"""Aggregate Aegis test results (surefire/failsafe XML, Playwright, k6) into Markdown.

Usage:
  python scripts/test-report.py <root> [-o report.md] [--playwright e2e/results.json]
      [--k6 evidence] [--branch NAME] [--commit SHA]

`root` is scanned for `**/surefire-reports/*.xml` and `**/failsafe-reports/*.xml`;
module names are derived from the directory structure.
"""
import argparse
import glob
import json
import os
import sys
import xml.etree.ElementTree as ET


def parse_suite(path):
    root = ET.parse(path).getroot()
    tests = int(root.get("tests", 0))
    failures = 0
    errors = 0
    for tc in root.iter("testcase"):
        if tc.find("failure") is not None:
            failures += 1
        if tc.find("error") is not None:
            errors += 1
    return {
        "name": root.get("name", os.path.basename(path)),
        "tests": tests,
        "failures": failures,
        "errors": errors,
        "skipped": int(root.get("skipped", 0)),
        "time": float(root.get("time", 0) or 0),
    }


SERVICE_PREFIXES = ["identity", "wallet", "bff", "audit", "fraud", "reporting", "common"]


def module_from_suite(name):
    # name is the fully qualified class, e.g. com.aegis.bff.BffServiceTest
    for svc in SERVICE_PREFIXES:
        if name.startswith(f"com.aegis.{svc}."):
            return "aegis-common" if svc == "common" else f"aegis-{svc}-service"
    return "unknown"


def scan_maven(root):
    modules = {}
    for path in glob.glob(os.path.join(root, "**", "*.xml"), recursive=True):
        # upload-artifact flattens paths, so attribute the module from the
        # testsuite package instead of the directory layout.
        try:
            root_el = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        if root_el.tag != "testsuite":
            continue
        suite = parse_suite(path)
        module = module_from_suite(suite["name"])
        kind = "IT" if "failsafe" in path.replace("\\", "/") else "unit"
        modules.setdefault(module, []).append((kind, suite))
    return modules


def render_maven(modules):
    rows = []
    tot_t, tot_f, tot_e, tot_s, tot_time = 0, 0, 0, 0, 0.0
    for module in sorted(modules):
        suites = modules[module]
        t = sum(s["tests"] for _, s in suites)
        f = sum(s["failures"] for _, s in suites)
        e = sum(s["errors"] for _, s in suites)
        s = sum(s["skipped"] for _, s in suites)
        tm = sum(s["time"] for _, s in suites)
        tot_t += t; tot_f += f; tot_e += e; tot_s += s; tot_time += tm
        kinds = ",".join(sorted({k for k, _ in suites}))
        rows.append(f"| {module} | {kinds} | {t} | {f} | {e} | {s} | {tm:.1f}s |")
    rows.append(f"| **TOTAL** | | **{tot_t}** | **{tot_f}** | **{tot_e}** | **{tot_s}** | {tot_time:.1f}s |")
    header = "| Module | Kind | Tests | Failures | Errors | Skipped | Time |\n|--------|------|-------|----------|--------|---------|------|"
    return header + "\n" + "\n".join(rows), tot_f + tot_e


def render_playwright(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            data = json.load(fh)
        stats = data.get("stats", {})
        suites = data.get("suites", [])
        total = stats.get("expected", 0) + stats.get("unexpected", 0) + stats.get("flaky", 0)
        return (
            f"| Result | Count |\n|--------|-------|\n"
            f"| Passed | {stats.get('expected', 0)} |\n"
            f"| Failed | {stats.get('unexpected', 0)} |\n"
            f"| Flaky | {stats.get('flaky', 0)} |\n"
            f"| **Total** | **{total}** |\n"
        ), stats.get("unexpected", 0)
    except (FileNotFoundError, json.JSONDecodeError):
        return "_(no Playwright report)_", 0


def render_k6(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return fh.read()[:4000], 0
    except FileNotFoundError:
        return "_(no k6 report)_", 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("root", help="root dir to scan for surefire/failsafe reports")
    ap.add_argument("-o", "--output", help="write report to this file")
    ap.add_argument("--playwright", help="path to Playwright JSON report")
    ap.add_argument("--k6", help="path to k6 RESULTS.md")
    ap.add_argument("--branch", default="")
    ap.add_argument("--commit", default="")
    args = ap.parse_args()

    out = [f"## Test Report"]
    if args.branch or args.commit:
        out.append(f"\n> Branch: `{args.branch}` · Commit: `{args.commit}`")
    out.append("\n### Unit & Integration (Maven)")
    modules = scan_maven(args.root)
    if modules:
        maven_md, maven_fail = render_maven(modules)
        out.append(maven_md)
    else:
        maven_fail = 0
        out.append("_(no Maven reports found)_")

    if args.playwright:
        out.append("\n### E2E (Playwright)")
        pw_md, pw_fail = render_playwright(args.playwright)
        out.append(pw_md)
    else:
        pw_fail = 0

    if args.k6:
        out.append("\n### Load (k6)")
        k6_md, _ = render_k6(args.k6)
        out.append(k6_md)

    report = "\n".join(out) + "\n"
    if args.output:
        with open(args.output, "w", encoding="utf-8") as fh:
            fh.write(report)
    else:
        print(report)

    sys.exit(1 if (maven_fail + pw_fail) > 0 else 0)


if __name__ == "__main__":
    main()
