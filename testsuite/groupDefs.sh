

##  Definitions of test groups; used in build and test scripts.

#API_TESTS="-Dapi.module"
#INTEGRATION_TESTS="-Dts.integration"

SMOKE_TESTS="-Dts.smoke"
BASIC_TESTS="-Dts.basic"
CLUSTER_TESTS="-Dts.clust"
IIOP_TESTS="-Dts.iiop"

BENCHMARK_TESTS="-Dbenchmark.module  -Dts.noSmoke"
STRESS_TESTS="-Dstress.module  -Dts.noSmoke"
DOMAIN_TESTS="-Ddomain.module  -Dts.noSmoke"
COMPAT_TESTS="-Dcompat.module  -Dts.noSmoke"

#ALL_TESTS="$INTEGRATION_TESTS $DOMAIN_TESTS $COMPAT_TESTS $SMOKE_TESTS"
#ALL_TESTS="$INTEGRATION_TESTS -Ddomain.module -Dcompat.module $SMOKE_TESTS"