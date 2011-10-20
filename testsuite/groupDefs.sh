

##  Definitions of test groups; used in build and test scripts.

API_TESTS="-Dapi.module"
BENCHMARK_TESTS="-Dbenchmark.module"
INTEGRATION_TESTS="-Dintegration.module -Dbasic.integration.tests -Dcompat.integration.tests -Dclustering.integration.tests -Dtimerservice.integration.tests"
SMOKE_TESTS="-Dintegration.module -Dsmoke.integration.tests"
SPEC_TESTS="-Dspec.module"
STRESS_TESTS="-Dstress.module"
DOMAIN_TESTS="-Ddomain.module"

ALL_TESTS="$INTEGRATION_TESTS $API_TESTS $SPEC_TESTS $DOMAIN_TESTS"