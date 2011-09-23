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
    [ -n "$JBOSS_HOME" ] &&
        JBOSS_HOME=`cygpath --unix "$JBOSS_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup JBOSS_HOME
JBOSS_HOME=`cd "$DIRNAME/../.."; pwd`
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

###
# Setup the JBoss Vault Tool classpath
###

# Shared libs
JBOSS_VAULT_CLASSPATH="$MODULEPATH/org/picketbox/main/*"
JBOSS_VAULT_CLASSPATH="$JBOSS_VAULT_CLASSPATH:$MODULEPATH/org/jboss/logging/main/*"
JBOSS_VAULT_CLASSPATH="$JBOSS_VAULT_CLASSPATH:$MODULEPATH/org/jboss/common-core/main/*"
JBOSS_VAULT_CLASSPATH="$JBOSS_VAULT_CLASSPATH:$MODULEPATH/org/jboss/as/security/main/*"

export JBOSS_VAULT_CLASSPATH

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JBOSS_CLASSPATH=`cygpath --path --windows "$JBOSS_CLASSPATH"`
    JBOSS_ENDORSED_DIRS=`cygpath --path --windows "$JBOSS_ENDORSED_DIRS"`
    MODULEPATH=`cygpath --path --windows "$MODULEPATH"`
    JBOSS_VAULT_CLASSPATH=`cygpath --path --windows "$JBOSS_VAULT_CLASSPATH"`
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  JBoss Vault"
echo ""
echo "  JBOSS_HOME: $JBOSS_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  VAULT Classpath: $JBOSS_VAULT_CLASSPATH"
echo "========================================================================="
echo ""

"$JAVA" -classpath "$JBOSS_VAULT_CLASSPATH" \
   org.jboss.as.security.vault.VaultTool
