#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# IMPORTANT:
# - Use JDK 11 (or JDK 8). JDK 21+ breaks compilation for commons-collections4 4.4.
# - This script runs a *FAST* PIT configuration, mutating only map/list/iterators packages.

TARGET_CLASSES="org.apache.commons.collections4.map.*,org.apache.commons.collections4.list.*,org.apache.commons.collections4.iterators.*"
PIT_GOAL="org.pitest:pitest-maven:mutationCoverage"

# Common PIT flags
COMMON_FLAGS=(
  -Drat.skip=true
  -Dthreads=5
  -DwithHistory
  -DtimestampedReports=false
)

echo "[1/2] Compiling tests..."
mvn -Drat.skip=true -q test-compile

mkdir -p pitReports

run_pit () {
  local label="$1"
  local targetTests="$2"
  local excluded="$3"

  echo ""
  echo "=== PIT: $label ==="
  rm -rf "pitReports/$label"
  mkdir -p "pitReports/$label"

  # Build the mvn command incrementally so we don't accidentally pass an empty targetTests
  local cmd=(mvn "${COMMON_FLAGS[@]}" -DreportsDirectory="pitReports/$label" -DtargetClasses="$TARGET_CLASSES")

  if [[ -n "$targetTests" ]]; then
    cmd+=( -DtargetTests="$targetTests" )
  fi
  if [[ -n "$excluded" ]]; then
    cmd+=( -DexcludedTestClasses="$excluded" )
  fi

  cmd+=( test-compile "$PIT_GOAL" )
  "${cmd[@]}"
}

# DEV = all developer tests (exclude our custom D*/M* helper suites)
# We intentionally *do not* set targetTests here so PIT discovers the full test suite.
run_pit "dev" "" "org.apache.commons.collections4.D*Test,org.apache.commons.collections4.M*Test"

# Developer-sampled suites
run_pit "sample-16" "org.apache.commons.collections4.D16Test" ""
run_pit "sample-32" "org.apache.commons.collections4.D32Test" ""
run_pit "sample-64" "org.apache.commons.collections4.D64Test" ""

# LLM suites
run_pit "llm-16" "org.apache.commons.collections4.M16Test" ""
run_pit "llm-32" "org.apache.commons.collections4.M32Test" ""
run_pit "llm-64" "org.apache.commons.collections4.M64Test" ""

echo ""
echo "Done. Open the HTML reports under: pitReports/*/index.html"
