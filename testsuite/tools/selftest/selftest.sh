

PROGNAME=`basename $0`
DIRNAME=`dirname $0`


RES_DIR=~/tmp/AS7-TS-selftest
IN='-DallTests -Dinteg-tests -Dcluster-tests -Dbasic-tests -Dbenchmark-tests -Dsmoke-tests -Dstress-tests -Ddomain-tests -Dcompat-tests';
for GROUP in $(echo $IN | tr ";" "\n"); do
  ## Create results dir.
  GRP_DIR=$RES_DIR/$GROUP
  mkdir -p $GRP_DIR
  ## Run the testsuite.
  ./integration-tests.sh clean -DallTests;
  if [ ! $? ] ; then echo "$GROUP: clean failed." >> $RES_DIR/log.txt; continue; fi;
  ./integration-tests.sh install $GROUP;
  if [ ! $? ] ; then echo "$GROUP: 1st run failed." >> $RES_DIR/log.txt; continue; fi;
  ## Archive the reports.
  for REPORT in `find testsuite -name 'TEST-*.xml' -or -name '*TestCase.txt' -or -name '*TestCase-output.txt'`; do
    SUB_DIR=`dirname $REPORT | sed 's#testsuite/##' | sed 's#/target/surefire-reports##'`
    mkdir -p $GRP_DIR/$SUB_DIR
    cp $REPORT $GRP_DIR/$SUB_DIR
  done
  ## Run the testsuite for 2nd time to ensure that unclean run works too.
  ./integration-tests.sh install $GROUP;
  if [ ! $? ] ; then echo "$GROUP: 2nd run failed." >> $RES_DIR/log.txt; continue; fi;
done

## Create a JUnitDiff report.
JUNITDIFF=${JUNITDIFF:-$DIRNAME/../junitdiff/JUnitDiff.jar}	
if [ -f $JUNITDIFF ] ; then
  java -jar $JUNITDIFF -o $RES_DIR/AS7-TS-GroupsComparison.html `ls -1 -c $RES_DIR`
fi


./build.sh -DallTests
./build.sh install -DallTests
./build.sh clean -DallTests
./build.sh clean install -DallTests
./integration-tests.sh -DallTests;
./integration-tests.sh install -DallTests;
./integration-tests.sh clean -DallTests;
./integration-tests.sh clean install -DallTests;
