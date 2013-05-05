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
    [ -n "$WILDFLY_HOME" ] &&
        WILDFLY_HOME=`cygpath --unix "$WILDFLY_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup WILDFLY_HOME
RESOLVED_WILDFLY_HOME=`cd "$DIRNAME/.."; pwd`
if [ "x$WILDFLY_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    WILDFLY_HOME=$RESOLVED_WILDFLY_HOME
else
 SANITIZED_WILDFLY_HOME=`cd "$WILDFLY_HOME"; pwd`
 if [ "$RESOLVED_WILDFLY_HOME" != "$SANITIZED_WILDFLY_HOME" ]; then
   echo "WARNING WILDFLY_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
 fi
fi
export WILDFLY_HOME

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$WILDFLY_HOME/modules"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    WILDFLY_HOME=`cygpath --path --windows "$WILDFLY_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
fi

eval \"$JAVA\" $JAVA_OPTS \
         -Djboss.home.dir=\"$WILDFLY_HOME\" \
         -jar \"$WILDFLY_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.jdr \
         "$@" 
