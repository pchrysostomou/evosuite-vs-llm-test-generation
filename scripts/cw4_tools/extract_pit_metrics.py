#!/usr/bin/env python3
# -*- coding: utf-8 -*-

\"\"\"Extract summary metrics from PIT reports.

Usage:
  python scripts/cw4_tools/extract_pit_metrics.py --project-root . --out pit_summary.csv

This reads:
  pitReports/<run>/index.html
  pitReports/<run>/mutations.xml

and produces a CSV with:
  run, classes, line_coverage_pct, line_coverage, mutation_coverage_pct, mutation_coverage,
  test_strength_pct, test_strength, total_mutants, killed, survived, no_coverage, timed_out
\"\"\"

from __future__ import annotations

import argparse
import csv
import os
import re
import xml.etree.ElementTree as ET
from typing import Dict, Tuple, Optional, List


PROJECT_SUMMARY_RE = re.compile(
    r\"Project Summary\\s+Number of Classes\\s+Line Coverage\\s+Mutation Coverage\\s+Test Strength\\s+\"
    r\"(?P<classes>\\d+)\\s+(?P<line_pct>\\d+)%\\s+(?P<line_frac>\\d+/\\d+)\\s+\"
    r\"(?P<mut_pct>\\d+)%\\s+(?P<mut_frac>\\d+/\\d+)\\s+(?P<ts_pct>\\d+)%\\s+(?P<ts_frac>\\d+/\\d+)\",
    re.MULTILINE
)


def parse_index_html(path: str) -> Dict[str, str]:
    html = open(path, \"r\", encoding=\"utf-8\", errors=\"ignore\").read()
    # Strip tags crudely to get searchable text
    text = re.sub(r\"<script[\\s\\S]*?</script>\", \" \", html, flags=re.IGNORECASE)
    text = re.sub(r\"<style[\\s\\S]*?</style>\", \" \", text, flags=re.IGNORECASE)
    text = re.sub(r\"<[^>]+>\", \" \", text)
    text = re.sub(r\"\\s+\", \" \", text).strip()

    m = PROJECT_SUMMARY_RE.search(text)
    if not m:
        raise ValueError(f\"Could not parse PIT summary from {path}\")

    return m.groupdict()


def parse_mutations_xml(path: str) -> Dict[str, int]:
    root = ET.parse(path).getroot()
    counts: Dict[str, int] = {}
    for mut in root.findall(\"mutation\"):
        s = mut.get(\"status\") or \"UNKNOWN\"
        counts[s] = counts.get(s, 0) + 1

    return {
        \"total\": sum(counts.values()),
        \"killed\": counts.get(\"KILLED\", 0),
        \"survived\": counts.get(\"SURVIVED\", 0),
        \"no_coverage\": counts.get(\"NO_COVERAGE\", 0),
        \"timed_out\": counts.get(\"TIMED_OUT\", 0),
    }


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument(\"--project-root\", default=\".\", help=\"Project root containing pitReports/\")
    ap.add_argument(\"--out\", default=\"pit_summary.csv\", help=\"Output CSV path\")
    ap.add_argument(
        \"--runs\",
        nargs=\"*\",
        default=[\"dev\", \"sample-16\", \"sample-32\", \"sample-64\", \"llm-16\", \"llm-32\", \"llm-64\"],
        help=\"Run folder names under pitReports/\",
    )
    args = ap.parse_args()

    pit_dir = os.path.join(args.project_root, \"pitReports\")
    rows: List[Dict[str, str]] = []

    for run in args.runs:
        run_dir = os.path.join(pit_dir, run)
        idx = os.path.join(run_dir, \"index.html\")
        mx = os.path.join(run_dir, \"mutations.xml\")
        if not os.path.exists(idx) or not os.path.exists(mx):
            print(f\"[WARN] Missing report for run '{run}' (expected {idx} and {mx}); skipping\")
            continue

        s = parse_index_html(idx)
        c = parse_mutations_xml(mx)

        row = {
            \"run\": run,
            \"classes\": s[\"classes\"],
            \"line_coverage_pct\": s[\"line_pct\"],
            \"line_coverage\": s[\"line_frac\"],
            \"mutation_coverage_pct\": s[\"mut_pct\"],
            \"mutation_coverage\": s[\"mut_frac\"],
            \"test_strength_pct\": s[\"ts_pct\"],
            \"test_strength\": s[\"ts_frac\"],
            \"total_mutants\": str(c[\"total\"]),
            \"killed\": str(c[\"killed\"]),
            \"survived\": str(c[\"survived\"]),
            \"no_coverage\": str(c[\"no_coverage\"]),
            \"timed_out\": str(c[\"timed_out\"]),
        }
        rows.append(row)

    if not rows:
        raise SystemExit(\"No runs parsed. Check pitReports directory and run names.\")

    fieldnames = list(rows[0].keys())
    with open(args.out, \"w\", newline=\"\", encoding=\"utf-8\") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)

    print(f\"Wrote: {args.out}\")


if __name__ == \"__main__\":
    main()
