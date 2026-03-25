# Commons Collections 4.4 — PIT-ready (fast)

This project is prepared so you can run PIT mutation testing **quickly** on a focused part of the codebase.

## 0) Java version (important)

**Use JDK 11 (or JDK 8).**  
JDK 21+ causes compilation errors in commons-collections4 4.4 because newer JDK interfaces add methods that clash with this source version.

Ubuntu example:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk maven
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## 1) What’s included

- `D16Test.java`, `D32Test.java`, `D64Test.java`  
  Developer-sampled **JUnit 4 suites** (disjoint sets of existing project tests).

- `M16Test.java`, `M32Test.java`, `M64Test.java`  
  New **JUnit 4** “LLM-style” tests:
  - M16: map-focused (16 tests)
  - M32: list-focused (32 tests)
  - M64: iterators-focused (64 tests)

- `run_all_pit_fast.sh`  
  Runs **7 PIT reports** fast, mutating only:
  - `org.apache.commons.collections4.map.*`
  - `org.apache.commons.collections4.list.*`
  - `org.apache.commons.collections4.iterators.*`

Reports are written to: `pitReports/dev`, `pitReports/sample-16|32|64`, `pitReports/llm-16|32|64`.

## 2) Run everything (fast)

```bash
chmod +x run_all_pit_fast.sh
./run_all_pit_fast.sh
```

Open HTML reports:
- `pitReports/dev/index.html`
- `pitReports/sample-16/index.html` (etc.)
- `pitReports/llm-64/index.html` (etc.)

## 3) Optional: full-library PIT

This is much slower:

```bash
chmod +x run_all_pit_full.sh
./run_all_pit_full.sh
```

Output: `pitReports/dev-full/index.html`

## 4) Manual PIT example

Map-only (like your command):

```bash
mvn -Drat.skip=true -Dthreads=5 -DtimestampedReports=false \
  -DtargetClasses="org.apache.commons.collections4.map.*" \
  -DtargetTests="org.apache.commons.collections4.map.*Test" \
  test-compile org.pitest:pitest-maven:mutationCoverage
```
