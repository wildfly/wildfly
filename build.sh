#!/bin/sh
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

#  Default search path for maven.
MAVEN_SEARCH_PATH="\
    tools
    tools/maven \
    tools/apache/maven \
    maven"



#  Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

#  OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
esac

#
#  Helper to complain.
#
die() {
    echo "${PROGNAME}: $*"
    exit 1
}

#
#  Helper to complain.
#
warn() {
    echo "${PROGNAME}: $*"
}

#
#  Helper to source a file if it exists.
#
source_if_exists() {
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
            #  Found.
            echo $MAVEN_HOME
            break
        fi
    done
}

#
#  Main function.
#
main() {
    #  If there is a build config file, source it.
    source_if_exists "$DIRNAME/build.conf" "$HOME/.build.conf"

    #  Increase the maximum file descriptors if we can.
    if [ $cygwin = "false" ]; then
        MAX_FD_LIMIT=`ulimit -H -n`
        if [ $? -eq 0 ]; then
            if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
                #  Use system's max.
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
            MAVEN_HOME=`find_maven $MAVEN_SEARCH_PATH`
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

    #  Setup some build properties
    MVN_OPTS="$MVN_OPTS -Dbuild.script=$0"

    #  Change to the directory where the script lives, so users are not forced
    #  to be in the same directory as build.xml.
    cd $DIRNAME


    . testsuite/groupDefs.sh

    #  Add smoke integration test directives before calling maven.
    MVN_SETTINGS_XML_ARGS="-s tools/maven/conf/settings.xml"
    TESTS=$SMOKE_TESTS
    MVN_GOAL="";
    ADDIT_PARAMS="";
    #  For each parameter, check for testsuite directives.
    for param in $@ ; do
        case $param in
            ## -s .../settings.xml - don't use our own.
            -s)      MVN_SETTINGS_XML_ARGS="";   ADDIT_PARAMS="$ADDIT_PARAMS $param";;
            -*)      ADDIT_PARAMS="$ADDIT_PARAMS $param";;
            clean)   MVN_GOAL="$MVN_GOAL$param ";;
            test)    MVN_GOAL="$MVN_GOAL$param ";;
            install) MVN_GOAL="$MVN_GOAL$param ";;
            deploy)  MVN_GOAL="$MVN_GOAL$param ";;
            site)    MVN_GOAL="$MVN_GOAL$param ";;
            *)       ADDIT_PARAMS="$ADDIT_PARAMS $param";;
        esac
    done
    #  Default goal if none specified.
    if [ -z "$MVN_GOAL" ]; then MVN_GOAL="install"; fi

    MVN_GOAL="$MVN_GOAL $TESTS"

    #  Export some stuff for maven.
    export MVN MAVEN_HOME MVN_OPTS MVN_GOAL

    # The default arguments.  `mvn -s ...` will override this.
    MVN_ARGS=${MVN_ARGS:-"$MVN_SETTINGS_XML_ARGS"};

    echo "$MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS"

    #  Execute in debug mode, or simply execute.
    if [ "x$MVN_DEBUG" != "x" ]; then
        /bin/sh -x $MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS
    else
        exec       $MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS
    fi
}

##
##  Bootstrap
##
main "$@"
