#!/bin/sh

# Use --debug to activate debug mode with an optional argument to specify the port.
# Usage : standalone.bat --debug
#         standalone.bat --debug 9797

# By default debug mode is disable.
DEBUG_MODE=false
DEBUG_PORT="8787"
SERVER_OPTS=""
while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          shift
          if [ -n "$1" ] && [ "${1#*-}" = "$1" ]; then
              DEBUG_PORT=$1
          else
              SERVER_OPTS="$SERVER_OPTS \"$1\""
          fi
          ;;
      --)
          shift
          break;;
      *)
          SERVER_OPTS="$SERVER_OPTS \"$1\""
          ;;
    esac
    shift
done

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
   echo ""
   echo "   WARNING:  JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
   echo "             JBOSS_HOME: $JBOSS_HOME"
   echo ""
   sleep 2s
 fi
fi
export JBOSS_HOME

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    RUN_CONF="$DIRNAME/standalone.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF"
fi

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT=`echo $JAVA_OPTS | $GREP "\-agentlib:jdwp"`
    if [ "x$DEBUG_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
    JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`

    # Check If server or client is specified
    SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
    CLIENT_SET=`echo $JAVA_OPTS | $GREP "\-client"`

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    CLIENT_VM=false
    if [ "x$CLIENT_SET" != "x" ]; then
        CLIENT_VM=true
    elif [ "x$SERVER_SET" = "x" ]; then
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            CLIENT_VM=true
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    if [ $CLIENT_VM = false ]; then
        NO_COMPRESSED_OOPS=`echo $JAVA_OPTS | $GREP "\-XX:\-UseCompressedOops"`
        if [ "x$NO_COMPRESSED_OOPS" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+UseCompressedOops -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+UseCompressedOops"
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$JBOSS_HOME/modules"
fi

if $linux; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $SERVER_OPTS"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d '"'`
       case $p in
         -Djboss.server.base.dir=*)
              JBOSS_BASE_DIR=`readlink -m ${p#*=}`
              ;;
         -Djboss.server.log.dir=*)
              JBOSS_LOG_DIR=`readlink -m ${p#*=}`
              ;;
         -Djboss.server.config.dir=*)
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
        -Djboss.server.base.dir=*)
             JBOSS_BASE_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.server.log.dir=*)
             JBOSS_LOG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.server.config.dir=*)
             JBOSS_CONFIG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
      esac
    done
fi

# No readlink -m on BSD
if $darwin; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $SERVER_OPTS"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d '"'`
       case $p in
         -Djboss.server.base.dir=*)
              JBOSS_BASE_DIR=`cd ${p#*=} ; pwd -P`
              ;;
         -Djboss.server.log.dir=*)
              JBOSS_LOG_DIR=`cd ${p#*=} ; pwd -P`
              ;;
         -Djboss.server.config.dir=*)
              JBOSS_CONFIG_DIR=`cd ${p#*=} ; pwd -P`
              ;;
       esac
    done
fi

# determine the default base dir, if not set
if [ "x$JBOSS_BASE_DIR" = "x" ]; then
   JBOSS_BASE_DIR="$JBOSS_HOME/standalone"
fi
# determine the default log dir, if not set
if [ "x$JBOSS_LOG_DIR" = "x" ]; then
   JBOSS_LOG_DIR="$JBOSS_BASE_DIR/log"
fi
# determine the default configuration dir, if not set
if [ "x$JBOSS_CONFIG_DIR" = "x" ]; then
   JBOSS_CONFIG_DIR="$JBOSS_BASE_DIR/configuration"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
    JBOSS_BASE_DIR=`cygpath --path --windows "$JBOSS_BASE_DIR"`
    JBOSS_LOG_DIR=`cygpath --path --windows "$JBOSS_LOG_DIR"`
    JBOSS_CONFIG_DIR=`cygpath --path --windows "$JBOSS_CONFIG_DIR"`
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
      eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/server.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         -jaxpmodule "javax.xml.jaxp-provider" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\"$JBOSS_HOME\" \
         -Djboss.server.base.dir=\"$JBOSS_BASE_DIR\" \
         "$SERVER_OPTS"
      JBOSS_STATUS=$?
   else
      # Execute the JVM in the background
      eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR/server.log\" \
         \"-Dlogging.configuration=file:$JBOSS_CONFIG_DIR/logging.properties\" \
         -jar \"$JBOSS_HOME/jboss-modules.jar\" \
         -mp \"${JBOSS_MODULEPATH}\" \
         -jaxpmodule "javax.xml.jaxp-provider" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\"$JBOSS_HOME\" \
         -Djboss.server.base.dir=\"$JBOSS_BASE_DIR\" \
         "$SERVER_OPTS" "&"
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
