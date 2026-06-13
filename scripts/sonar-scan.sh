#!/usr/bin/env bash
set -uo pipefail

PROJECT_KEY="${1:-auth-service-java}"
PROJECT_NAME="${2:-Auth Service Java}"
TEST_TIMEOUT="${TEST_TIMEOUT:-600}"
EXCLUSIONS="${EXCLUSIONS:-**/target/**,**/db/migration/**}"
SONAR_TOKEN="${SONAR_TOKEN:-squ_733ffb0b40e059b7a5f353a7f16c2896cb8e7052}"
SONAR_HOST="${SONAR_HOST:-http://localhost:9000}"
SCANNER_BIN="${SCANNER_BIN:-/home/teilor/.sonar/native-sonar-scanner/sonar-scanner-6.2.1.4610-linux-x64/bin/sonar-scanner}"

export LANG="${LANG:-C.UTF-8}"
export LC_ALL="${LC_ALL:-C.UTF-8}"
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1024m}"

echo "=========================================="
echo " SonarQube scan"
echo "  Project:    $PROJECT_KEY"
echo "  Name:       $PROJECT_NAME"
echo "=========================================="

rm -rf .sonarqube
rm -f target/jacoco-report/jacoco.generic.xml

echo ""
echo ">> Step 1/2: Maven test + coverage"
set +e
timeout "$TEST_TIMEOUT" ./mvnw clean verify -DforkCount=1 -DreuseForks=true -Dnet.bytebuddy.experimental=true || echo "WARN: tests reported failures (exit $?)"
set -e

echo ""
echo ">> Step 2/2: SonarQube scan"
rm -rf .sonarqube
"$SCANNER_BIN" \
  -Dsonar.host.url="$SONAR_HOST" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.projectKey="$PROJECT_KEY" \
  -Dsonar.projectName="$PROJECT_NAME" \
  -Dsonar.sources="src/main/java" \
  -Dsonar.tests="src/test/java" \
  -Dsonar.exclusions="$EXCLUSIONS" \
  -Dsonar.java.coveragePlugin=jacoco \
  -Dsonar.jacoco.reportPaths="target/jacoco-report/jacoco.xml" || echo "WARN: scanner failed (exit $?)"

echo ""
echo "=========================================="
echo " Done. Dashboard:"
echo "  $SONAR_HOST/dashboard?id=$PROJECT_KEY"
echo "=========================================="
