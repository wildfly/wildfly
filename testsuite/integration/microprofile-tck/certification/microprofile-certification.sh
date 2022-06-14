#!/bin/sh

readonly MP_VERSION=${1}
readonly BASE_DIR=${2:-$PWD}
readonly JDK_VERSION=$(mvn -v | grep "Java version" | sed "s/Java version: //" | sed "s/,.*//")

set -euo pipefail

is_defined() {
  local var=${1}
  local msg=${2:-'A required value is missing'}
  local status=${3:-'1'}

  if [ -z "${var}" ]; then
    echo "${msg}"
    exit "${status}"
  fi
}

sum_header_parameter() {
  local parameter=${1}
  local result

  result=$(for file in $(find ./*); do (grep "<testsuite " $file || true) | sed "s/.*$parameter=\"//" | sed "s/\".*//"; done | awk '{s+=$1} END {print s}')

  echo $result
}

parse_testsuite_header() {
  cd target/surefire-reports/junitreports

  local time=$(sum_header_parameter time)
  local tests=$(sum_header_parameter tests)
  local errors=$(sum_header_parameter errors)
  local skipped=$(sum_header_parameter skipped)
  local failures=$(sum_header_parameter failures)

  echo "<testsuite name=\"TestSuite\" time=\"$time\" tests=\"$tests\" errors=\"$errors\" skipped=\"$skipped\" failures=\"$failures\">"
}

parse_testcases() {
  local result

  cd target/surefire-reports/junitreports

  for file in $(find ./*); do
    testcases=$(grep "<testcase" $file || true)
    result+="$testcases\n"
  done

  echo -e "$result"
}

translate_to_spec_name() {
  local spec=${1}

  case $spec in
  "config")
    echo "Config"
    ;;
  "fault-tolerance")
    echo "Fault Tolerance"
    ;;
  "health")
    echo "Health"
    ;;
  "jwt")
    echo "JWT Propagation"
    ;;
  "metrics")
    echo "Metrics"
    ;;
  "openapi")
    echo "OpenAPI"
    ;;
  "opentracing")
    echo "OpenTracing"
    ;;
  "rest-client")
    echo "REST Client"
    ;;
  *)
    return 1
    ;;
  esac
}

parse_test_results() {
  local output_file=${1}
  local spec=${2}
  local spec_api_artifact=${3:-""}
  local version

  cd $spec

  local local_repo=""

  if [ ! -z "${MAVEN_REPO_LOCAL:-}" ]
  then
    local_repo="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
  fi

  version=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate "-Dexpression=version.$spec" -q -DforceStdout $local_repo)

  tee -a "${FILE}" <<EOF
== MicroProfile $(translate_to_spec_name $spec) $version

Test results:

[source,xml]
----
<?xml version="1.0"?>
$(parse_testsuite_header)
$(parse_testcases)
</testsuite>
----

EOF

  cd ..
}

is_defined "${MP_VERSION}" "No MicroProfile version provided as an argument" 1

rm -rf $PWD/target
mkdir $PWD/target

readonly FILE="$PWD/target/microprofile-${MP_VERSION}-jdk-${JDK_VERSION}.adoc"
touch "$FILE"

tee -a "${FILE}" <<EOF
= Platform TCK Test results JDK ${JDK_VERSION}

== Environment

[source,bash]
----
$ mvn -version
$(mvn -version)
----

EOF
cd "$BASE_DIR"

# metrics results need to be moved to junitreports directory
mkdir -p "metrics/target/surefire-reports/junitreports"
cp metrics/target/surefire-reports/TEST* metrics/target/surefire-reports/junitreports/

parse_test_results "$FILE" "config"
parse_test_results "$FILE" "fault-tolerance"
parse_test_results "$FILE" "health"
parse_test_results "$FILE" "jwt" "jwt-auth"
parse_test_results "$FILE" "metrics"
parse_test_results "$FILE" "openapi"
parse_test_results "$FILE" "opentracing"
parse_test_results "$FILE" "rest-client"
