#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Compute (approximate) pairwise intersections between the coursework suites.

We treat a test case as a test method:
  - For M*Test: each @Test method in the suite class
  - For D*Test: expand the JUnit suite and collect test methods from referenced classes

Outputs a CSV of |A ∩ B|, |A|, |B| and Jaccard index.

Usage:
  python scripts/cw4_tools/suite_intersections.py --project-root . --out intersections.csv
"""

from __future__ import annotations

import argparse
import csv
import os
import re
from dataclasses import dataclass
from typing import List, Dict, Set, Tuple


TEST_ROOT_DEFAULT = os.path.join("src", "test", "java")


@dataclass(frozen=True)
class TestCase:
    id: str


def fqcn_to_path(test_root: str, fqcn: str) -> str:
    return os.path.join(test_root, *fqcn.split(".")) + ".java"


def read_text(path: str) -> str:
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def extract_junit4_ids(java_src: str, class_id: str) -> List[str]:
    ids: List[str] = []
    for m in re.finditer(r"@Test\b", java_src):
        start = m.end()
        sig = re.search(r"\bpublic\s+void\s+([A-Za-z0-9_]+)\s*\(", java_src[start:])
        if not sig:
            continue
        ids.append(f"{class_id}#{sig.group(1)}")
    return ids


def extract_junit3_ids(java_src: str, class_id: str) -> List[str]:
    return [f"{class_id}#{m.group(1)}" for m in re.finditer(r"\bpublic\s+void\s+(test[A-Za-z0-9_]+)\s*\(", java_src)]


def extract_test_ids_from_class(test_root: str, fqcn: str) -> List[str]:
    src = read_text(fqcn_to_path(test_root, fqcn))
    ids = extract_junit4_ids(src, fqcn)
    return ids if ids else extract_junit3_ids(src, fqcn)


def expand_suite_classes(java_src: str) -> List[str]:
    m = re.search(r"@Suite\.SuiteClasses\s*\(\s*\{\s*([^}]+)\s*\}\s*\)", java_src, re.DOTALL)
    if not m:
        return []
    body = m.group(1)
    return re.findall(r"([A-Za-z0-9_$.]+)\s*\.class", body)


def resolve_imports(java_src: str) -> Dict[str, str]:
    imports: Dict[str, str] = {}
    for line in java_src.splitlines():
        line = line.strip()
        if line.startswith("import ") and line.endswith(";"):
            fq = line[len("import "):-1].strip()
            simple = fq.split(".")[-1]
            imports[simple] = fq
    return imports


def suite_members_to_fqcn(java_src: str) -> List[str]:
    pkg_m = re.search(r"^\s*package\s+([A-Za-z0-9_.]+)\s*;", java_src, re.MULTILINE)
    pkg = pkg_m.group(1) if pkg_m else ""
    imports = resolve_imports(java_src)

    members = expand_suite_classes(java_src)
    fq_members: List[str] = []
    for token in members:
        if "." in token:
            fq_members.append(token.replace("$", "."))
            continue
        if token in imports:
            fq_members.append(imports[token].replace("$", "."))
        elif pkg:
            fq_members.append(f"{pkg}.{token}".replace("$", "."))
        else:
            fq_members.append(token.replace("$", "."))
    return fq_members


def suite_test_ids(test_root: str, suite_fqcn: str) -> Set[str]:
    suite_src = read_text(fqcn_to_path(test_root, suite_fqcn))
    direct = extract_junit4_ids(suite_src, suite_fqcn)
    if direct:
        return set(direct)

    # suite of classes
    ids: Set[str] = set()
    for member in suite_members_to_fqcn(suite_src):
        try:
            ids.update(extract_test_ids_from_class(test_root, member))
        except FileNotFoundError:
            continue
    return ids


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project-root", default=".", help="Project root")
    ap.add_argument("--test-root", default=TEST_ROOT_DEFAULT, help="Test sources root (relative to project root)")
    ap.add_argument("--out", default="intersections.csv", help="Output CSV")
    ap.add_argument(
        "--suites",
        nargs="*",
        default=[
            "org.apache.commons.collections4.M16Test",
            "org.apache.commons.collections4.M32Test",
            "org.apache.commons.collections4.M64Test",
            "org.apache.commons.collections4.D16Test",
            "org.apache.commons.collections4.D32Test",
            "org.apache.commons.collections4.D64Test",
        ],
        help="Suite FQCNs",
    )
    args = ap.parse_args()
    test_root = os.path.join(args.project_root, args.test_root)

    suite_ids: Dict[str, Set[str]] = {s: suite_test_ids(test_root, s) for s in args.suites}

    suites = list(suite_ids.keys())
    rows = []
    for i in range(len(suites)):
        for j in range(i+1, len(suites)):
            a, b = suites[i], suites[j]
            A, B = suite_ids[a], suite_ids[b]
            inter = len(A & B)
            union = len(A | B)
            jacc = (inter / union) if union else 0.0
            rows.append([a, b, len(A), len(B), inter, union, f"{jacc:.6f}"])

    with open(args.out, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["suite_a", "suite_b", "|A|", "|B|", "|A∩B|", "|A∪B|", "jaccard"])
        w.writerows(rows)

    print(f"Wrote: {args.out}")


if __name__ == "__main__":
    main()
