#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Compute an approximate I-TSDm (Input Test Set Diameter) for the coursework suites.

Implements the multiset NCD definition from Feldt et al. ("Test set diameter..."):

  NCD1(X) = ( C(X) - min_{x∈X} C(x) ) / max_{x∈X} C(X \ {x})

and approximates:

  NCD(X) ≈ max( NCD1(Y_k) ) over a nested chain of subsets Y_k built by repeatedly
           removing the element whose removal maximises NCD1.

C(s) is taken as gzip-compressed byte length of UTF-8 text.

By default, test inputs are extracted as *test method bodies* from:
  - org.apache.commons.collections4.M16Test / M32Test / M64Test (JUnit4, @Test methods)
and for the developer sample suites:
  - org.apache.commons.collections4.D16Test / D32Test / D64Test (JUnit4 suites),
    where we expand the suite's referenced classes and extract their test methods.

Usage:
  python scripts/cw4_tools/compute_it_sdm.py --project-root . --out it_sdm.csv
"""

from __future__ import annotations

import argparse
import gzip
import os
import re
import csv
from dataclasses import dataclass
from typing import List, Sequence, Dict


TEST_ROOT_DEFAULT = os.path.join("src", "test", "java")


@dataclass(frozen=True)
class TestCaseText:
    id: str
    text: str


def fqcn_to_path(test_root: str, fqcn: str) -> str:
    return os.path.join(test_root, *fqcn.split(".")) + ".java"


def read_text(path: str) -> str:
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def gzip_len(s: str) -> int:
    b = s.encode("utf-8", errors="ignore")
    return len(gzip.compress(b))


def concat_for_multiset(items: Sequence[str]) -> str:
    # Feldt et al. note concatenation in increasing length (and lexicographic tie-break).
    ordered = sorted(items, key=lambda x: (len(x), x))
    return "\n---\n".join(ordered)


def C_multiset(items: Sequence[str]) -> int:
    return gzip_len(concat_for_multiset(items))


def ncd1(texts: Sequence[str]) -> float:
    n = len(texts)
    if n <= 1:
        return 0.0
    Cx = [gzip_len(t) for t in texts]
    C_X = C_multiset(texts)
    min_Cx = min(Cx)

    # Denominator: max over removing 1 element
    max_C_X_minus = 0
    for i in range(n):
        subset = [texts[j] for j in range(n) if j != i]
        max_C_X_minus = max(max_C_X_minus, C_multiset(subset))

    if max_C_X_minus == 0:
        return 0.0

    return (C_X - min_Cx) / max_C_X_minus


def ncd_multiset_approx(texts: Sequence[str]) -> float:
    # Nested-chain approximation (O(n^2) evaluations of ncd1)
    if len(texts) <= 2:
        return ncd1(texts)

    current = list(texts)
    best = ncd1(current)

    while len(current) > 2:
        best_next = -1.0
        best_remove_idx = 0
        for i in range(len(current)):
            subset = [current[j] for j in range(len(current)) if j != i]
            val = ncd1(subset)
            if val > best_next:
                best_next = val
                best_remove_idx = i

        best = max(best, best_next)
        current.pop(best_remove_idx)

    best = max(best, ncd1(current))
    return best


def extract_junit4_tests(java_src: str, class_id: str) -> List[TestCaseText]:
    # Find occurrences of @Test, then capture the following method body (brace matching)
    results: List[TestCaseText] = []
    for m in re.finditer(r"@Test\b", java_src):
        start = m.end()
        sig = re.search(r"\bpublic\s+void\s+([A-Za-z0-9_]+)\s*\(", java_src[start:])
        if not sig:
            continue
        name = sig.group(1)
        sig_start = start + sig.start()

        brace_open = java_src.find("{", sig_start)
        if brace_open == -1:
            continue

        depth = 0
        i = brace_open
        while i < len(java_src):
            ch = java_src[i]
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    body = java_src[brace_open:i+1]
                    results.append(TestCaseText(f"{class_id}#{name}", body))
                    break
            i += 1
    return results


def extract_junit3_tests(java_src: str, class_id: str) -> List[TestCaseText]:
    results: List[TestCaseText] = []
    for m in re.finditer(r"\bpublic\s+void\s+(test[A-Za-z0-9_]+)\s*\(", java_src):
        name = m.group(1)
        sig_start = m.start()

        brace_open = java_src.find("{", sig_start)
        if brace_open == -1:
            continue

        depth = 0
        i = brace_open
        while i < len(java_src):
            ch = java_src[i]
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    body = java_src[brace_open:i+1]
                    results.append(TestCaseText(f"{class_id}#{name}", body))
                    break
            i += 1
    return results


def extract_tests_from_class(test_root: str, fqcn: str) -> List[TestCaseText]:
    path = fqcn_to_path(test_root, fqcn)
    src = read_text(path)
    tests = extract_junit4_tests(src, fqcn)
    if tests:
        return tests
    return extract_junit3_tests(src, fqcn)


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


def suite_members_to_fqcn(java_src: str, suite_fqcn: str) -> List[str]:
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


def load_suite_testcases(test_root: str, suite_fqcn: str) -> List[TestCaseText]:
    suite_path = fqcn_to_path(test_root, suite_fqcn)
    suite_src = read_text(suite_path)

    direct = extract_junit4_tests(suite_src, suite_fqcn)
    if direct:
        return direct

    members = suite_members_to_fqcn(suite_src, suite_fqcn)
    all_tests: List[TestCaseText] = []
    for member in members:
        try:
            all_tests.extend(extract_tests_from_class(test_root, member))
        except FileNotFoundError:
            continue
    return all_tests


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project-root", default=".", help="Project root")
    ap.add_argument("--test-root", default=TEST_ROOT_DEFAULT, help="Test sources root (relative to project root)")
    ap.add_argument("--out", default="it_sdm.csv", help="Output CSV")
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
        help="Suite FQCNs to evaluate",
    )
    args = ap.parse_args()

    test_root = os.path.join(args.project_root, args.test_root)

    rows = []
    for suite in args.suites:
        tests = load_suite_testcases(test_root, suite)
        texts = [t.text for t in tests if t.text.strip()]
        it_sdm = ncd_multiset_approx(texts) if texts else 0.0
        rows.append((suite, len(texts), it_sdm))
        print(f"{suite}: cases={len(texts)} I-TSDm≈{it_sdm:.4f}")

    with open(args.out, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["suite", "num_test_cases", "it_sdm_approx"])
        for suite, n, val in rows:
            w.writerow([suite, n, f"{val:.6f}"])

    print(f"Wrote: {args.out}")


if __name__ == "__main__":
    main()
