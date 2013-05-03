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
solaris=false;
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
    SunOS*)
        solaris=true
        ;;
esac

# Read an optional running configuration file
if [ "x$DOMAIN_CONF" = "x" ]; then
    DOMAIN_CONF="$DIRNAME/domain.conf"
fi
if [ -r "$DOMAIN_CONF" ]; then
    . "$DOMAIN_CONF"
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
RESOLVED_JBOSS_HOME=`cd "$DIRNAME/.."; pwd`
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    JBOSS_HOME=$RESOLVED_JBOSS_HOME
else
 SANITIZED_JBOSS_HOME=`cd "$JBOSS_HOME"; pwd`
 if [ "$RESOLVED_JBOSS_HOME" != "$SANITIZED_JBOSS_HOME" ]; then
   echo "WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
 fi
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
    if [ "x$HAS_OPENJDK" = "x" ]; then
        HAS_OPENJDK=`"$JAVA" $JVM_OPTVERSION 2>&1 | $GREP -i OpenJDK`
    fi

    # Check for IBM JVM w/server support
    if [ "x$HAS_IBM" = "x" ]; then
        HAS_IBM=`"$JAVA" $JVM_OPTVERSION 2>&1 | $GREP -i "IBM J9"`
    fi

    # Enable -server if we have Hotspot or OpenJDK, unless we can't
    if [ "x$HAS_HOTSPOT" != "x" -o "x$HAS_OPENJDK" != "x" -o "x$HAS_IBM" != "x" ]; then
        # MacOS does not support -server flag
        if [ "$darwin" != "true" ]; then
            PROCESS_CONTROLLER_JAVA_OPTS="-server $PROCESS_CONTROLLER_JAVA_OPTS"
            HOST_CONTROLLER_JAVA_OPTS="-server $HOST_CONTROLLER_JAVA_OPTS"
            JVM_OPTVERSION="-server $JVM_OPTVERSION"
        fi
    fi
else
    JVM_OPTVERSION="-server $JVM_OPTVERSION"
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$JBOSS_HOME/modules"
fi

if $linux; then
    # consolidate the host-controller and command line opts
    HOST_CONTROLLER_OPTS="$HOST_CONTROLLER_JAVA_OPTS $@"
    # process the host-controller options
    for var in $HOST_CONTROLLER_OPTS
    do
       # Remove quotes
      p=`echo $var | tr -d '"'`
      case $p in
        -Djboss.domain.base.dir=*)
             JBOSS_BASE_DIR=`readlink -m ${p#*=}`
             ;;
        -Djboss.domain.log.dir=*)
             JBOSS_LOG_DIR=`readlink -m ${p#*=}`
             ;;
        -Djboss.domain.config.dir=*)
             JBOSS_CONFIG_DIR=`readlink -m ${p#*=}`
             ;;
      esac
    done
fi

if $solaris; then
    # consolidate the host-controller and command line opts
    HOST_CONTROLLER_OPTS="$HOST_CONTROLLER_JAVA_OPTS $@"
    # process the host-controller options
    for var in $HOST_CONTROLLER_OPTS
    do
       # Remove quotes
      p=`echo $var | tr -d '"'`
      case $p in
        -Djboss.domain.base.dir=*)
             JBOSS_BASE_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.domain.log.dir=*)
             JBOSS_LOG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.domain.config.dir=*)
             JBOSS_CONFIG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
      esac
    done
fi

# No readlink -m on BSD
if $darwin; then
    # consolidate the host-controller and command line opts
    HOST_CONTROLLER_OPTS="$HOST_CONTROLLER_JAVA_OPTS $@"
    # process the host-controller options
    for var in $HOST_CONTROLLER_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d '"'`
       case $p in
        -Djboss.domain.base.dir=*)
             JBOSS_BASE_DIR=`cd ${p#*=} ; pwd -P`
             ;;
        -Djboss.domain.log.dir=*)
             JBOSS_LOG_DIR=`cd ${p#*=} ; pwd -P`
             ;;
        -Djboss.domain.config.dir=*)
             JBOSS_CONFIG_DIR=`cd ${p#*=} ; pwd -P`
             ;;
      esac
    done
fi
# determine the default base dir, if not set
if [ "x$JBOSS_BASE_DIR" = "x" ]; then
   JBOSS_BASE_DIR="$JBOSS_HOME/domain"
fi
# determine the default log dir, if not set
if [ "x$JBOSS_LOG_DIR" = "x" ]; then
   JBOSS_LOG_DIR="$JBOSS_BASE_DIR/log"
fi
# determine the default configuration dir, if not set
if [ "x$JBOSS_CONFIG_DIR" = "x" ]; then
   JBOSS_CONFIG_DIR="$JBOSS_BASE_DIR/configuration"
fi

# Setup the java path to invoke from JVM
# Needed to start domain from cygwin when the JAVA path will result in an invalid path
JAVA_FROM_JVM="$JAVA"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JAVA_FROM_JVM=`cygpath --path --absolute --windows "$JAVA_FROM_JVM"`
    JBOSS_BASE_DIR=`cygpath --path --windows "$JBOSS_BASE_DIR"`
    JBOSS_LOG_DIR=`cygpath --path --windows "$JBOSS_LOG_DIR"`
    JBOSS_CONFIG_DIR=`cygpath --path --windows "$JBOSS_CONFIG_DIR"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  JBoss Bootstrap Environment"
echo ""
echo "  JBOSS_HOME: $JBOSS_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

while true; do
   if [ "x$LAUNCH_JBOSS_IN_BACKGROUND" = "x" ]; then
      # Execute the JVM in the foreground
      eval \"$JAVA\" -D\"[Process Controller]\" $PROCESS_CONTROLLER_JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/process-controller.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.process-controller \
         -jboss-home \"$JBOSS_HOME\" \
         -jvm \"$JAVA_FROM_JVM\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         -- \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/host-controller.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         $HOST_CONTROLLER_JAVA_OPTS \
         -- \
         -default-jvm \"$JAVA_FROM_JVM\" \
         '"$@"'
      JBOSS_STATUS=$?
   else
      # Execute the JVM in the background
      eval \"$JAVA\" -D\"[Process Controller]\" $PROCESS_CONTROLLER_JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/process-controller.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         org.jboss.as.process-controller \
         -jboss-home \"$JBOSS_HOME\" \
         -jvm \"$JAVA_FROM_JVM\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         -- \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/host-controller.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         $HOST_CONTROLLER_JAVA_OPTS \
         -- \
         -default-jvm \"$JAVA_FROM_JVM\" \
         '"$@"' "&"
      JBOSS_PID=$!
      # Trap common signals and relay them to the jboss process
      trap "kill -HUP  $JBOSS_PID" HUP
      trap "kill -TERM $JBOSS_PID" INT
      trap "kill -QUIT $JBOSS_PID" QUIT
      trap "kill -PIPE $JBOSS_PID" PIPE
      trap "kill -TERM $JBOSS_PID" TERM
      if [ "x$JBOSS_PIDFILE" != "x" ]; then
        echo $JBOSS_PID > $JBOSS_PIDFILE
      fi
      # Wait until the background process exits
      WAIT_STATUS=128
      while [ "$WAIT_STATUS" -ge 128 ]; do
         wait $JBOSS_PID 2>/dev/null
         WAIT_STATUS=$?
         if [ "$WAIT_STATUS" -gt 128 ]; then
            SIGNAL=`expr $WAIT_STATUS - 128`
            SIGNAL_NAME=`kill -l $SIGNAL`
            echo "*** JBossAS process ($JBOSS_PID) received $SIGNAL_NAME signal ***" >&2
         fi
      done
      if [ "$WAIT_STATUS" -lt 127 ]; then
         JBOSS_STATUS=$WAIT_STATUS
      else
         JBOSS_STATUS=0
      fi
      if [ "$JBOSS_STATUS" -ne 10 ]; then
            # Wait for a complete shudown
            wait $JBOSS_PID 2>/dev/null
      fi
      if [ "x$JBOSS_PIDFILE" != "x" ]; then
            grep "$JBOSS_PID" $JBOSS_PIDFILE && rm $JBOSS_PIDFILE
      fi
   fi
   if [ "$JBOSS_STATUS" -eq 10 ]; then
      echo "Restarting JBoss..."
   else
      exit $JBOSS_STATUS
   fi
done
