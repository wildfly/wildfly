#!/bin/bash

##  
##  This is a JBoss AS testsuite self-test.
##  It's purpose is for testsuite harness development.
##  It should run various parameters configuration for two branches and compare their results.
##  If they differ, this script fails.
##  
##  Not fully implemented yet.
##  


PROGNAME=`basename $0`
DIRNAME=`dirname $0`

COMMIT_OLD=${1:upstream/master}
COMMIT_NEW=$2

RES_DIR=~/tmp/AS7-TS-selftest

##
##  Run test runs for various groups. Runs them twice to validate there's no damaging garbage left.
##
function RunTestRuns {
  COMMIT_ID=$1;  ##  De-facto dir name for reports.
  if [ "" == "$1" ] ; then echo "ERROR: Commit ID not set for RunTestRuns()."; return 1; fi
  #COMMIT=$2;     ##  Git commit (can be also a branch, tag, etc.)
  #if [ "" == "$2" ] ; then echo "ERROR: Commit not set for RunTestRuns()."; return 2; fi
  
  IN='-DallTests -Dinteg-tests -Dcluster-tests -Dbasic-tests -Dbenchmark-tests -Dsmoke-tests -Dstress-tests -Ddomain-tests -Dcompat-tests';
  for GROUP in $(echo $IN | tr ";" "\n"); do
    ## Create results dir.
    GRP_DIR=$RES_DIR/$COMMIT_ID/$GROUP
	 rm -rf $GRP_DIR
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

  ##  Test what these commands pass to maven.
  ./build.sh -DonlyShowMvnCommand               -DallTests    > $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./build.sh -DonlyShowMvnCommand       install -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./build.sh -DonlyShowMvnCommand clean         -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./build.sh -DonlyShowMvnCommand clean install -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./integration-tests.sh -DonlyShowMvnCommand               -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./integration-tests.sh -DonlyShowMvnCommand       install -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./integration-tests.sh -DonlyShowMvnCommand clean         -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt
  ./integration-tests.sh -DonlyShowMvnCommand clean install -DallTests   >> $RES_DIR/$COMMIT_ID/mvnCommands.txt

}


##
## Create a JUnitDiff report.
##
function createJUnitDiffReport {
  JUNITDIFF=${JUNITDIFF:-$DIRNAME/../junitdiff/JUnitDiff.jar}
  if [ -f $JUNITDIFF ] ; then
    java -jar $JUNITDIFF -o $RES_DIR/AS7-TS-GroupsComparison.html `ls -1 -c $RES_DIR`
  fi
}


#
## --- main --- ##
#
function main {
  ##  Build AS. Don't care about unit tests.
  ./build.sh clean install -DskipTests
  
  ##  Run the test with what we currently have.
  RunTestRuns new $COMMIT_NEW
  if [ "" != $COMMIT_OLD ] ; then
    git checkout $COMMIT_OLD
    RunTestRuns old $COMMIT_OLD
    git checkout $COMMIT_NEW   ## Restore
    ##  Diff the generated mvn commands; Warn / log if changed.
    if [ `diff -q $RES_DIR/old/mvnCommands.txt $RES_DIR/new/mvnCommands.txt` ] ; then
      echo "WARN: Scripts produced different mvn commands.";
    fi
  fi

}

main $@