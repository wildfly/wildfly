#!/bin/bash
### ====================================================================== ###
##                                                                          ##
##  This is the main entry point for the build system.                      ##
##                                                                          ##
##  Users should execute this file rather than 'mvn' to ensure              ##
##  the correct version is being used with the correct configuration.       ##
##                                                                          ##
### ====================================================================== ###

# $Id: build.sh 105735 2010-06-04 19:45:13Z pgier $

PROGNAME=`basename $0`
DIRNAME=`dirname $0`
GREP="grep"
ROOT="/"

# Ignore user's MAVEN_HOME if it is set
M2_HOME=""
MAVEN_HOME=""

MAVEN_OPTS="$MAVEN_OPTS -Xmx512M"
export MAVEN_OPTS

# The default search path for Maven.
MAVEN_SEARCH_PATH="\
    tools
    tools/maven \
    tools/apache/maven \
    maven"

# The default arguments.
MVN_OPTIONS="-s ../tools/maven/conf/settings.xml"

# Use the maximum available, or set MAX_FD != -1 to use that.
MAX_FD="maximum"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
case "`uname`" in
    CYGWIN*)  cygwin=true;;
    Darwin*)  darwin=true;;
esac




#
#  Integration testsuite support.
#

#
CMD_LINE_PARAMS=
TESTS_SPECIFIED="N"
. testsuite/groupDefs.sh

#
# Helper to process command line for test directives
# - user-specified parameters (allTests, stress-tests, benchmark-tests) are translated into the appropriate
# maven build profiles and removed from the command line
# - smoke tests run with build
#
process_test_directives() {

    MVN_GOALS="";

    # For each parameter, check for testsuite directives.
    for param in $@ ; do
    case $param in
      ## -DallTests runs all tests except benchmark and stress.
      -DallTests)        TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS -DallTests -fae";;

      -Dinteg-tests)     TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $INTEGRATION_TESTS";;
      -Dcluster-tests)   TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $CLUSTER_TESTS";;
      -Dsmoke-tests)     TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $SMOKE_TESTS";;
      -Dbasic-tests)     TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $BASIC_TESTS";;
      -Ddomain-tests)    TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $DOMAIN_TESTS";;
      -Dcompat-tests)    TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $COMPAT_TESTS";;
      -Dbenchmark-tests) TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $BENCHMARK_TESTS";;
      -Dstress-tests)    TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $STRESS_TESTS";;
      ## Don't run smoke tests if a single test is specified.
      -Dtest=*)          TESTS_SPECIFIED="Y";  CMD_LINE_PARAMS="$CMD_LINE_PARAMS $param";; # -DfailIfNoTests=false

      ## Collect Maven goals.
      clean)   MVN_GOALS="$MVN_GOALS$param ";;
      test)    MVN_GOALS="$MVN_GOALS$param ";;
      install) MVN_GOALS="$MVN_GOALS$param ";;
      deploy)  MVN_GOALS="$MVN_GOALS$param ";;
      site)    MVN_GOALS="$MVN_GOALS$param ";;
      ## Pass through all other params.
      *)      CMD_LINE_PARAMS="$CMD_LINE_PARAMS $param";;
    esac
    done

    #  Default goal if none specified.
    if [ -z "$MVN_GOALS" ]; then MVN_GOALS="install"; fi
    CMD_LINE_PARAMS="$MVN_GOALS $CMD_LINE_PARAMS";

    # If no tests specified, run smoke tests.
    if [[ $TESTS_SPECIFIED == "N" ]]; then
        CMD_LINE_PARAMS="$CMD_LINE_PARAMS $SMOKE_TESTS"
    fi
}

#
# Helper to complain.
#
die() {
    echo "${PROGNAME}: $*"
    exit 1
}

#
# Helper to complain.
#
warn() {
    echo "${PROGNAME}: $*"
}

#
# Helper to source a file if it exists.
#
maybe_source() {
    for file in $*; do
        if [ -f "$file" ]; then
            . $file
        fi
    done
}

find_maven() {
    search="$*"
    for d in $search; do
        MAVEN_HOME="`pwd`/$d"
        MVN="$MAVEN_HOME/bin/mvn"
        if [ -x "$MVN" ]; then
            # found one
            echo $MAVEN_HOME
            break
        fi
    done
}

#
# Main function.
#
main() {
    #  If there is a build config file. then source it.
    maybe_source "$DIRNAME/build.conf" "$HOME/.build.conf"

    #  Increase the maximum file descriptors if we can.
    if [ $cygwin = "false" ]; then
        MAX_FD_LIMIT=`ulimit -H -n`
        if [ $? -eq 0 ]; then
            if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
                #  Use the system max.
                MAX_FD="$MAX_FD_LIMIT"
            fi

            ulimit -n $MAX_FD
            if [ $? -ne 0 ]; then
                warn "Could not set maximum file descriptor limit: $MAX_FD"
            fi
        else
            warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
        fi
    fi

    #  Try the search path.
    MAVEN_HOME=`find_maven $MAVEN_SEARCH_PATH`

    #  Try looking up to root.
    if [ "x$MAVEN_HOME" = "x" ]; then
        target="build"
        _cwd=`pwd`

        while [ "x$MAVEN_HOME" = "x" ] && [ "$cwd" != "$ROOT" ]; do
            cd ..
            cwd=`pwd`
            MAVEN_HOME=`search $MAVEN_SEARCH_PATH`
        done

        #  Make sure we get back.
        cd $_cwd

        if [ "$cwd" != "$ROOT" ]; then
            found="true"
        fi

        #  Complain if we did not find anything.
        if [ "$found" != "true" ]; then
            die "Could not locate Maven; check \$MVN or \$MAVEN_HOME."
        fi
    fi

    #  Make sure we have one.
    MVN=$MAVEN_HOME/bin/mvn
    if [ ! -x "$MVN" ]; then
        die "Maven file is not executable: $MVN"
    fi

    #  Need to specify planet57/buildmagic protocol handler package.
    MVN_OPTS="-Djava.protocol.handler.pkgs=org.jboss.net.protocol"

    #  Setup some build properties.
    MVN_OPTS="$MVN_OPTS -Dbuild.script=$0"

    #  Change to the directory where the script lives 
    #  so users are not forced to be in the same directory as build.xml.
    cd $DIRNAME/testsuite

    MVN_GOAL=$@
    if [ -z "$MVN_GOAL" ]; then
        MVN_GOAL="install"
    fi

    #  Process test directives before calling maven.
    process_test_directives $MVN_GOAL
    MVN_GOAL=$CMD_LINE_PARAMS

    # Export some stuff for maven.
    export MVN MAVEN_HOME MVN_OPTS MVN_GOAL

    echo "$MVN $MVN_OPTIONS $MVN_GOAL"

    #  Execute in debug mode, or simply execute.
    if [ "x$MVN_DEBUG" != "x" ]; then
        /bin/sh -x $MVN $MVN_OPTIONS $MVN_GOAL
    else
        exec $MVN $MVN_OPTIONS $MVN_GOAL
    fi

    cd $DIRNAME
}

##
## Bootstrap
##

main "$@"
