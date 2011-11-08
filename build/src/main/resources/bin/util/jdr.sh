#!/bin/sh

# JBoss Diagnostic Reporter (JDR)
#
# This script creates a JDR report containing useful information for
# diagnosing problems with the application server.  The report consists 
# of a zip file containing log files, configuration, a list of all files
# in the distribution and, if available, runtime metrics.
#

DIRNAME=`dirname "$0"`

# OS specific support (must be 'true' or 'false').
cygwin=false;
if  [ `uname|grep -i CYGWIN` ]; then
    cygwin = true;
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$JBOSS_HOME" ] &&
        JBOSS_HOME=`cygpath --unix "$JBOSS_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup JBOSS_HOME
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    JBOSS_HOME=`cd "$DIRNAME/../.."; pwd`
fi
export JBOSS_HOME

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "x$MODULEPATH" = "x" ]; then
    MODULEPATH="$JBOSS_HOME/modules"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JBOSS_CLASSPATH=`cygpath --path --windows "$JBOSS_CLASSPATH"`
    JBOSS_ENDORSED_DIRS=`cygpath --path --windows "$JBOSS_ENDORSED_DIRS"`
    MODULEPATH=`cygpath --path --windows "$MODULEPATH"`
fi

eval \"$JAVA\" $JAVA_OPTS \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${MODULEPATH}\" \
         -logmodule "org.jboss.logmanager" \
         org.jboss.as.jdr \
         -Djboss.home.dir=\"$JBOSS_HOME\" \
         "$@" 
