#!/bin/sh

DIRNAME=`dirname "$0"`
PROGNAME=`basename "$0"`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;

    Linux)
        linux=true
        ;;
esac

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    RUN_CONF="$DIRNAME/appclient.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF"
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

# Check for -d32/-d64 in JAVA_OPTS
JVM_OPTVERSION="-version"
JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`
test "x$JVM_D64_OPTION" != "x" && JVM_OPTVERSION="-d64 $JVM_OPTVERSION"
test "x$JVM_D32_OPTION" != "x" && JVM_OPTVERSION="-d32 $JVM_OPTVERSION"

# If -server not set in JAVA_OPTS, set it, if supported
SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
if [ "x$SERVER_SET" = "x" ]; then

    # Check for SUN(tm) JVM w/ HotSpot support
    if [ "x$HAS_HOTSPOT" = "x" ]; then
        HAS_HOTSPOT=`"$JAVA" $JVM_OPTVERSION -version 2>&1 | $GREP -i HotSpot`
    fi

    # Check for OpenJDK JVM w/server support
    if [ "x$HAS_OPENJDK_" = "x" ]; then
        HAS_OPENJDK=`"$JAVA" $JVM_OPTVERSION 2>&1 | $GREP -i OpenJDK`
    fi

    # Enable -server if we have Hotspot or OpenJDK, unless we can't
    if [ "x$HAS_HOTSPOT" != "x" -o "x$HAS_OPENJDK" != "x" ]; then
        # MacOS does not support -server flag
        if [ "$darwin" != "true" ]; then
            JAVA_OPTS="-server $JAVA_OPTS"
            JVM_OPTVERSION="-server $JVM_OPTVERSION"
        fi
    fi
else
    JVM_OPTVERSION="-server $JVM_OPTVERSION"
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$WILDFLY_HOME/modules"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    WILDFLY_HOME=`cygpath --path --windows "$WILDFLY_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
fi

CLASSPATH="$CLASSPATH:$WILDFLY_HOME/jboss-modules.jar"

# Execute the JVM in the foreground
eval \"$JAVA\" $JAVA_OPTS \
 -cp "$CLASSPATH" \
 \"-Dorg.jboss.boot.log.file=$WILDFLY_HOME/appclient/log/appclient.log\" \
 \"-Dlogging.configuration=file:$WILDFLY_HOME/appclient/configuration/logging.properties\" \
 org.jboss.modules.Main \
 -mp \"${JBOSS_MODULEPATH}\" \
 org.jboss.as.appclient \
 -Djboss.home.dir=\"$WILDFLY_HOME\" \
 -Djboss.server.base.dir=\"$WILDFLY_HOME/appclient\" \
 '"$@"'
JBOSS_STATUS=$?
exit $JBOSS_STATUS