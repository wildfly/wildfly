#!/bin/sh

DIRNAME=`dirname $0`

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
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    JBOSS_HOME=`cd $DIRNAME/..; pwd`
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

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# See if --quiet was specified to suppress env display
quiet="false"
for arg in "$@"
do
    if [ "$arg" = "--quiet" ]; then
        quiet="true"
    fi
done
if [ $quiet = "false" ]; then
# Display our environment
    echo "========================================================================="
    echo ""
    echo "  JBoss Admin Command-line Interface"
    echo ""
    echo "  JBOSS_HOME: $JBOSS_HOME"
    echo ""
    echo "  JAVA: $JAVA"
    echo ""
    echo "  JAVA_OPTS: $JAVA_OPTS"
    echo ""
    echo "========================================================================="
    echo ""
fi

eval \"$JAVA\" $JAVA_OPTS -jar \"$JBOSS_HOME/jboss-modules.jar\" -mp \"$JBOSS_HOME/modules\" org.jboss.as.cli '"$@"'
