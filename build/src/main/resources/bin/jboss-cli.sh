#!/bin/sh

DIRNAME=`dirname "$0"`

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

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    WILDFLY_HOME=`cygpath --path --windows "$WILDFLY_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

if $darwin ; then
    # Add the apple gui packages for the gui client
    JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=com.apple.laf,com.apple.laf.resources"
else
    # Add base package for L&F
    JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=com.sun.java.swing"
fi

# Sample JPDA settings for remote socket debugging
#JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n"

eval \"$JAVA\" $JAVA_OPTS \"-Dlogging.configuration=file:$WILDFLY_HOME/bin/jboss-cli-logging.properties\" -jar \"$WILDFLY_HOME/jboss-modules.jar\" -mp \"$WILDFLY_HOME/modules\" org.jboss.as.cli '"$@"'
