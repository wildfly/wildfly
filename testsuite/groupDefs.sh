

##  Definitions of test groups; used in build and test scripts.

#API_TESTS="-Dapi.module"
BENCHMARK_TESTS="-Dbenchmark.module -DnoSmoke"
#INTEGRATION_TESTS="-Dintegration.module -Dbasic.integration.tests -Dcompat.integration.tests -Dclustering.integration.tests -Dtimerservice.integration.tests"
INTEGRATION_TESTS="-Dintegration.module -Dbasic.integration.tests -Dclustering.integration.tests -Diiop.integration.tests"
BASIC_TESTS="-Dintegration.module -Dbasic.integration.tests  -DnoSmoke"
CLUSTER_TESTS="-Dintegration.module -Dclustering.integration.tests -DnoSmoke"
SMOKE_TESTS="-Dintegration.module" # -Dsmoke.integration.tests"
#SPEC_TESTS="-Dspec.module -DnoSmoke"
STRESS_TESTS="-Dstress.module  -DnoSmoke"
DOMAIN_TESTS="-Ddomain.module  -DnoSmoke"
COMPAT_TESTS="-Dcompat.module  -DnoSmoke"

#ALL_TESTS="$INTEGRATION_TESTS $DOMAIN_TESTS $COMPAT_TESTS $SMOKE_TESTS"
ALL_TESTS="$INTEGRATION_TESTS -Ddomain.module -Dcompat.module $SMOKE_TESTS"