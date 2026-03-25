# COMP0103 CW4 – Running PIT reports (Commons Collections 4.4)

## 1) Required “plain” PIT command (spec 1.1)

From the project root, this must work **without extra flags**:

```bash
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage
```

In this repo, the PIT plugin is configured to:
- mutate **all production classes** under `org.apache.commons.collections4.*`
- write the developer report to: `pitReports/dev/`
- avoid timestamped folders (`timestampedReports=false`)
- exclude the coursework suites `M*Test` and `D*Test` from the default (developer) run

So after the plain command you should have:

```
pitReports/dev/index.html
pitReports/dev/mutations.xml
```

## 2) Generate the 6 required suite reports (spec 1.5/1.7)

Run these from the project root.  
(We override `reportsDirectory` and clear `excludedTestClasses` so the suite class can run.)

### LLM suites
```bash
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/llm-16 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.M16Test
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/llm-32 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.M32Test
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/llm-64 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.M64Test
```

### Developer-sampled suites
```bash
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/sample-16 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.D16Test
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/sample-32 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.D32Test
mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=pitReports/sample-64 -DtimestampedReports=false -DexcludedTestClasses= -DtargetTests=org.apache.commons.collections4.D64Test
```

## 3) Auto-extract PIT table values for the report (optional but recommended)

After you have all 7 PIT runs in `pitReports/`, you can generate a CSV with all the values
needed for the tables:

```bash
python scripts/cw4_tools/extract_pit_metrics.py --project-root . --out pit_summary.csv
```

## 4) Reproducible I‑TSDm (spec 1.6)

Compute an approximate I‑TSDm (multiset NCD using gzip) for the 6 suites:

```bash
python scripts/cw4_tools/compute_it_sdm.py --project-root . --out it_sdm.csv
```

The script extracts test *method bodies* from each suite (expanding `D*Test` suites to the
referenced developer test classes) and computes an NCD‑based set diameter.


## 5) Reproducible suite intersection (spec 1.5)

Generate a simple intersection/Jaccard table between suites (based on extracted test methods):

```bash
python scripts/cw4_tools/suite_intersections.py --project-root . --out intersections.csv
```
