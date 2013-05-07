#!/bin/sh

DIRNAME=`dirname "$0"`
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
        echo "JAVA_HOME is not set. Unable to locate the jars needed to run jconsole."
        exit 2
    fi
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$WILDFLY_HOME/modules"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    WILDFLY_HOME=`cygpath --path --windows "$WILDFLY_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
fi

CLASSPATH=$JAVA_HOME/lib/jconsole.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar

MODULES="org/jboss/remoting-jmx org/jboss/remoting3 org/jboss/logging org/jboss/xnio org/jboss/xnio/nio org/jboss/sasl org/jboss/marshalling org/jboss/marshalling/river org/jboss/as/cli org/jboss/staxmapper org/jboss/as/protocol org/jboss/dmr org/jboss/as/controller-client org/jboss/threads"

for MODULE in $MODULES
do
    for JAR in `cd "$JBOSS_MODULEPATH/system/layers/base/$MODULE/main/" && ls -1 *.jar`
    do
        CLASSPATH="$CLASSPATH:$JBOSS_MODULEPATH/system/layers/base/$MODULE/main/$JAR"
    done
done


echo CLASSPATH $CLASSPATH

jconsole -J-Djava.class.path="$CLASSPATH"
