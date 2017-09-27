#!/bin/bash -e
### ====================================================================== ###
##                                                                          ##
##  A build script                                                          ##
##                                                                          ##
##  Note that in the past, this script took the following responsibilities  ##
##  that are now handled by ./mvnw (a.k.a. Maven Wrapper) or Maven itself:  ##
##                                                                          ##
##  * Download and install a specific version of Maven                      ##
##  * Set Maven options via MAVEN_OPTS environment variable - these can     ##
##    now be set in .mvn/jvm.config and .mvn/maven.config                   ##
##                                                                          ##
##  The only task left in this script is settting a sufficient limit for    ##
##  the open files (a.k.a. ulimit -n). If this is not important to you,     ##
##  feel free to use ./mvnw directly                                        ##
##                                                                          ##
### ====================================================================== ###
BASH_INTERPRETER=${BASH_INTERPRETER:-${SHELL}}

PROGNAME=`basename $0`
DIRNAME=`dirname "${BASH_SOURCE[0]}"`
DIRNAME=`cd "$DIRNAME" && pwd`
GREP="grep"
ROOT="/"

# Ignore user's MAVEN_HOME if it is set
M2_HOME=""
MAVEN_HOME=""

# MAVEN_OPTS now live in .mvn/jvm.config and .mvn/maven.config
# MAVEN_OPTS="$MAVEN_OPTS -Xmx1024M"
# export MAVEN_OPTS

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

#
#  Main function.
#
main() {
    #  If there is a build config file, source it.
    source_if_exists "$DIRNAME/build.conf" "$HOME/.build.conf"

    #  Increase the maximum file descriptors if we can.
    if [ $cygwin = "false" ]; then
        if MAX_FD_LIMIT=`ulimit -H -n`; then
            if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
                #  Use system's max.
                MAX_FD="$MAX_FD_LIMIT"
            fi

            if ! ulimit -n $MAX_FD; then
                warn "Could not set maximum file descriptor limit: $MAX_FD"
            fi
        else
            warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
        fi
    fi

    MVN="$DIRNAME/mvnw"

    #  Need to specify planet57/buildmagic protocol handler package.
    MVN_OPTS="-Djava.protocol.handler.pkgs=org.jboss.net.protocol"

    #  Setup some build properties
    MVN_OPTS="$MVN_OPTS -Dbuild.script=$0"

    #  Change to the directory where the script lives, so users are not forced
    #  to be in the same directory as build.xml.
    cd $DIRNAME

    # Add default settings.xml file if it exists
    MVN_SETTINGS_XML_DEFAULT="$DIRNAME/tools/maven/conf/settings.xml"
    if [ -f "$MVN_SETTINGS_XML_DEFAULT" ]; then
        MVN_SETTINGS_XML_ARGS="-s $MVN_SETTINGS_XML_DEFAULT"
    else
        MVN_SETTINGS_XML_ARGS=""
    fi
    MVN_GOAL="";
    ADDIT_PARAMS="";
    #  For each parameter, check for testsuite directives.
    for param in "$@" ; do
        case $param in
            ## -s .../settings.xml - don't use our own.
            -s)      MVN_SETTINGS_XML_ARGS="";   ADDIT_PARAMS="$ADDIT_PARAMS $param";;
            -*)      ADDIT_PARAMS="$ADDIT_PARAMS '$param'";;
            help*)   MVN_GOAL="$MVN_GOAL$param ";;
            clean)   MVN_GOAL="$MVN_GOAL$param ";;
            test)    MVN_GOAL="$MVN_GOAL$param ";;
            install) MVN_GOAL="$MVN_GOAL$param ";;
            deploy)  MVN_GOAL="$MVN_GOAL$param ";;
            site)    MVN_GOAL="$MVN_GOAL$param ";;
            *)       ADDIT_PARAMS="$ADDIT_PARAMS '$param'";;
        esac
    done
    #  Default goal if none specified.
    if [ -z "$MVN_GOAL" ]; then MVN_GOAL="install"; fi

    #  Export some stuff for maven.
    export MVN MAVEN_HOME MVN_OPTS MVN_GOAL

    # The default arguments.  `mvn -s ...` will override this.
    MVN_ARGS=${MVN_ARGS:-"$MVN_SETTINGS_XML_ARGS"};

    # WFLY-8175 requires that we keep installing Maven under the tools directory
    # the current project, at least when mvnw is invoked from build and integration-tests
    # scripts
    MVN_ARGS="-Dmaven.user.home=$DIRNAME/tools $MVN_ARGS"

    echo "$MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS"

    #  Execute in debug mode, or simply execute.
    if [ "x$MVN_DEBUG" != "x" ]; then
        eval "${BASH_INTERPRETER}" -x $MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS
    else
        eval exec ${BASH_INTERPRETER} $MVN $MVN_ARGS $MVN_GOAL $ADDIT_PARAMS
    fi
}

##
##  Bootstrap
##
main "$@"
