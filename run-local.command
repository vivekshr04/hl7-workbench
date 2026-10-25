#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf target/classes
mkdir -p target/classes
find src/main/java -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d target/classes
cp -R src/main/resources/. target/classes/
java -cp target/classes com.eqe.hl7workbench.HL7Workbench
