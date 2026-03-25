# commons-collections4-4.4-src — PIT (Mutation Testing) quick commands (Ubuntu)

## 0) Use Java 11 (required)

```bash
sudo apt update
sudo apt install -y openjdk-11-jdk maven

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

## 1) Run unit tests (skip RAT)

```bash
mvn -Drat.skip=true clean test
```

## 2) Run PIT for different test suites (reports into pitReports/...) 

All commands below also skip RAT.

### Developer full suite

```bash
mvn -Drat.skip=true -DtimestampedReports=false \
  -DreportsDirectory=pitReports/dev \
  test-compile org.pitest:pitest-maven:mutationCoverage
```

### Sampled developer suites (replace package if yours differs)

```bash
mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.D16Test*" \
  -DreportsDirectory=pitReports/sample-16 \
  test-compile org.pitest:pitest-maven:mutationCoverage

mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.D32Test*" \
  -DreportsDirectory=pitReports/sample-32 \
  test-compile org.pitest:pitest-maven:mutationCoverage

mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.D64Test*" \
  -DreportsDirectory=pitReports/sample-64 \
  test-compile org.pitest:pitest-maven:mutationCoverage
```

### LLM suites

```bash
mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.M16Test*" \
  -DreportsDirectory=pitReports/llm-16 \
  test-compile org.pitest:pitest-maven:mutationCoverage

mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.M32Test*" \
  -DreportsDirectory=pitReports/llm-32 \
  test-compile org.pitest:pitest-maven:mutationCoverage

mvn -Drat.skip=true -DtimestampedReports=false \
  -DtargetTests="org.apache.commons.collections4.M64Test*" \
  -DreportsDirectory=pitReports/llm-64 \
  test-compile org.pitest:pitest-maven:mutationCoverage
```

## 3) Open a report

```bash
xdg-open pitReports/dev/index.html
```

(Adjust the folder name for sample-16, llm-16, etc.)
