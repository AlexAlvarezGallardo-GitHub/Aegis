#!/usr/bin/env python3
"""Compare a k6 summary-export JSON against a perf baseline and fail on regressions.

Usage:
  python scripts/perf-check.py --summary perf-pr.json --baseline baseline.json \
      [--threshold 1.25] [-o report.md]

A regression is flagged when the PR p95 for a tracked endpoint exceeds
baseline p95 * threshold.
"""
import argparse
import json
import sys

TRACKED = [
    ("login", "aegis_login_latency"),
    ("list_wallets", "aegis_list_wallets_latency"),
    ("deposit", "aegis_deposit_latency"),
]


def p95_ms(data, metric):
    m = data.get("metrics", {}).get(metric, {})
    # k6 summary-export shapes vary: values either nested under "values" or flat.
    return m.get("p(95)") or (m.get("values") or {}).get("p(95)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--summary", required=True, help="k6 summary-export JSON for this run")
    ap.add_argument("--baseline", required=True, help="k6 summary-export JSON baseline (from main)")
    ap.add_argument("--threshold", type=float, default=1.25, help="allowed p95 ratio (default 1.25)")
    ap.add_argument("-o", "--output", help="write report to this file")
    args = ap.parse_args()

    summary = json.load(open(args.summary, encoding="utf-8"))
    baseline = json.load(open(args.baseline, encoding="utf-8"))

    rows = []
    failed = False
    for label, metric in TRACKED:
        cur = p95_ms(summary, metric)
        base = p95_ms(baseline, metric)
        if cur is None or base is None or base <= 0:
            rows.append(f"| {label} | - | {cur if cur is not None else '?'} ms | {base if base is not None else '?'} ms | - | N/A |")
            continue
        ratio = cur / base
        ok = ratio <= args.threshold
        if not ok:
            failed = True
        rows.append(
            f"| {label} | p95 | {cur:.0f} ms | {base:.0f} ms | {ratio:.2f}x | "
            f"{'PASS' if ok else 'FAIL regression'} |"
        )

    md = [
        "## Performance Check",
        "",
        f"> Threshold: PR p95 <= baseline p95 * {args.threshold:g}",
        "",
        "| Endpoint | Metric | PR p95 | Baseline p95 | Ratio | Status |",
        "|----------|--------|--------|--------------|-------|--------|",
    ] + rows + [""]

    report = "\n".join(md)
    if args.output:
        with open(args.output, "w", encoding="utf-8") as fh:
            fh.write(report)
    else:
        print(report)

    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
