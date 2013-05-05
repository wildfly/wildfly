#!/bin/sh

DIRNAME=`dirname "$0"`
PROGNAME=`basename "$0"`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

#
# Helper to complain.
#
warn() {
    echo "${PROGNAME}: $*"
}

#
# Helper to puke.
#
die() {
    warn $*
    exit 1
}

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
# Setup WILDFLY_HOME
RESOLVED_WILDFLY_HOME=`cd "$DIRNAME/.."; pwd`
if [ "x$WILDFLY_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    WILDFLY_HOME=$RESOLVED_WILDFLY_HOME
else
 SANITIZED_WILDFLY_HOME=`cd "$WILDFLY_HOME/.."; pwd`
 if [ "$RESOLVED_JBOSS" != "$SANITIZED_WILDFLY_HOME" ]; then
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

###
# Setup the JBoss Vault Tool classpath
###


# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    WILDFLY_HOME=`cygpath --path --windows "$WILDFLY_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  JBoss Vault"
echo ""
echo "  WILDFLY_HOME: $WILDFLY_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "========================================================================="
echo ""

eval \"$JAVA\" $JAVA_OPTS \
         -jar \"$WILDFLY_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.vault-tool \
         '"$@"'

