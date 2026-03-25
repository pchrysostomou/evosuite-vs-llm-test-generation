#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

cd "$(dirname "$0")"

# Full PIT (mutate the whole library). This can take a long time.
TARGET_CLASSES="org.apache.commons.collections4.*"
ALL_TESTS="org.apache.commons.collections4.*Test,org.apache.commons.collections4.**.*Test"
PIT_GOAL="org.pitest:pitest-maven:mutationCoverage"

COMMON_FLAGS=(-Dthreads=5
  -DtimestampedReports=false
)

mvn -q test-compile

mkdir -p pitReports

mvn "${COMMON_FLAGS[@]}" \
  -DreportsDirectory="pitReports/dev-full" \
  -DtargetClasses="$TARGET_CLASSES" \
  -DtargetTests="$ALL_TESTS" \
  -DexcludedTestClasses="org.apache.commons.collections4.D*Test,org.apache.commons.collections4.M*Test" \
  test-compile "$PIT_GOAL"

echo "Done. Open: pitReports/dev-full/index.html"
