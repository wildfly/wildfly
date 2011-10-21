

##  Definitions of test groups; used in build and test scripts.

#API_TESTS="-Dapi.module"
BENCHMARK_TESTS="-Dbenchmark.module"
#INTEGRATION_TESTS="-Dintegration.module -Dbasic.integration.tests -Dcompat.integration.tests -Dclustering.integration.tests -Dtimerservice.integration.tests"
INTEGRATION_TESTS="-Dintegration.module -Dbasic.integration.tests -Dclustering.integration.tests"
SMOKE_TESTS="-Dintegration.module -Dsmoke.integration.tests"
#SPEC_TESTS="-Dspec.module"
STRESS_TESTS="-Dstress.module"
DOMAIN_TESTS="-Ddomain.module"

ALL_TESTS="$INTEGRATION_TESTS $DOMAIN_TESTS $SMOKE_TESTS"