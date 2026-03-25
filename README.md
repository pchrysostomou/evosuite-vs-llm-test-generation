# EvoSuite vs LLM Test Generation Comparison

## Overview
This repository contains a study and empirical comparison between human-written (Developer) test suites, sampled test suites, and Large Language Model (LLM) generated test suites. The target project used for this analysis is **Apache Commons Collections 4**.

The primary objective is to evaluate test suite quality using **Mutation Testing** with [PIT (PIT Mutation Testing)](http://pitest.org/). The assignment evaluates the effectiveness, line coverage, and mutation coverage (test strength) of different test generation approaches.

## Project Structure
The repository is based on the Apache Commons Collections source code, with specific modifications and additions for this assignment:
* `pom.xml`: Modified to integrate the `pitest-maven` plugin. The analysis is specifically scoped to `org.apache.commons.collections4.*`.
* `src/test/java/org/apache/commons/collections4/`: Contains the original developer tests, alongside the newly added LLM-generated tests (`M16Test`, `M32Test`, `M64Test`) and developer sampled tests (`D16Test`, `D32Test`, `D64Test`).
* `run_all_pit_full.sh` / `run_all_pit_fast.sh`: Shell scripts added to automate the execution of PIT across all test variations.
* `target/pitReports/`: Directory where all the generated PIT HTML reports are stored (e.g., `dev`, `llm-16`, `sample-64`).

## Prerequisites
To run the mutation tests locally, ensure you have the following installed:
* **Java JDK** (Version 8 or higher)
* **Apache Maven**

## How to Run the Mutation Tests

The mutation tests can be executed using Maven. A property `${pit.report.dir}` is used to route the HTML reports to distinct directories so they don't overwrite each other.

### 1. Run the Original Developer Suite (Baseline)

    mvn clean compile test-compile org.pitest:pitest-maven:mutationCoverage

### 2. Run LLM Generated Suites (e.g., M16)
To run the mutation analysis exclusively for the LLM generated suite of size 16 (`M16Test`):

    mvn -Dpit.report.dir=pitReports/llm-16 -DtimestampedReports=false -DexcludedTestClasses -DtargetTests=org.apache.commons.collections4.M16Test clean compile test-compile org.pitest:pitest-maven:mutationCoverage

*(Change `M16Test` and `llm-16` to 32 or 64 to run the other LLM suites).*

### 3. Run Developer Sampled Suites (e.g., D16)
To run the mutation analysis exclusively for the Developer sampled suite of size 16 (`D16Test`):

    mvn -Dpit.report.dir=pitReports/sample-16 -DtimestampedReports=false -DexcludedTestClasses -DtargetTests=org.apache.commons.collections4.D16Test clean compile test-compile org.pitest:pitest-maven:mutationCoverage

### 4. Automated Execution
You can also run all suites sequentially using the provided bash scripts:

    chmod +x run_all_pit_fast.sh
    ./run_all_pit_fast.sh

## Summary of Results

Below is a summary of the PIT execution results comparing the different test suites based on 267 classes mapped from the `report.pdf`.

| Report Name                 | Classes | Line Coverage | Mutation Coverage |
|-----------------------------|---------|---------------|-------------------|
| **Developer suite (Dev)**| 267        | 44%           | 38%               |
| **LLM suite (M16)** | 267             | 4%            | 3%                |
| **LLM suite (M32)** | 267             | 3%            | ~3%               |
| **LLM suite (M64)** | 267             | 3%            | ~3%               |
| **Developer sampled (D16)** | 267     | 23%           | ~20%              |

*(Note: Detailed results, including KILLED, SURVIVED, NO_COVERAGE, and TIMED_OUT metrics, can be found in the generated HTML reports under `/target/pitReports/`).*
