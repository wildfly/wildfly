#!/bin/sh

# Use --debug to activate debug mode with an optional argument to specify the port.
# Usage : server.sh --debug
#         server.sh --debug 9797
# By default debug mode is disabled.
DEBUG_MODE="${DEBUG:-false}"
DEBUG_PORT="${DEBUG_PORT:-8787}"

# Use --jmx to activate JMX remoting mode with an optional argument to specify the port.
# Usage : server.sh --jmx
#         server.sh --jmx 1234
# By default JMX remoting is disabled.
JMX_REMOTING="${JMX:-false}"
JMX_PORT="${JMX_PORT:-9999}"

GC_LOG="$GC_LOG"
JAVA_OPTS_EXTRA=""
PROPERTIES=""
while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          if [ -n "$2" ] && [ "$2" = $(echo "$2" | sed 's/-//') ]; then
              DEBUG_PORT=$2
              shift
          fi
          ;;
      --jmx)
          JMX_REMOTING=true
          if [ -n "$2" ] && [ "$2" = $(echo "$2" | sed 's/-//') ]; then
              JMX_PORT=$2
              shift
          fi
          ;;
      --)
          shift
          break;;
      -s)
          ISPN_ROOT_DIR="$2"
          ARGUMENTS="$ARGUMENTS '$1' '$2'"
          shift
          ;;
      -D*)
          JAVA_OPTS_EXTRA="$JAVA_OPTS_EXTRA '$1'"
          ;;
      -P)
          if [ ! -f "$2" ]; then
            echo "Could not load property file: $2"
            exit
          fi
          while read -r LINE; do
            PROPERTIES="$PROPERTIES '-D$LINE'"
          done < "$2"
          ARGUMENTS="$ARGUMENTS '$1' '$2'"
          shift
          ;;
      *)
          ARGUMENTS="$ARGUMENTS '$1'"
          ;;
    esac
    shift
done
echo "$PROPERTIES"

GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

# tell linux glibc how many memory pools can be created that are used by malloc
MALLOC_ARENA_MAX="${MALLOC_ARENA_MAX:-1}"
export MALLOC_ARENA_MAX

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
solaris=false;
freebsd=false;
other=false
case "$(uname)" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
    FreeBSD)
        freebsd=true
        ;;
    Linux)
        linux=true
        ;;
    SunOS*)
        solaris=true
        ;;
    *)
        other=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$ISPN_HOME" ] &&
        ISPN_HOME=$(cygpath --unix "$ISPN_HOME")
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=$(cygpath --unix "$JAVAC_JAR")
fi

# Setup ISPN_HOME
RESOLVED_ISPN_HOME=$(cd "$DIRNAME/.." >/dev/null; pwd)
if [ "x$ISPN_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    ISPN_HOME=$RESOLVED_ISPN_HOME
else
  SANITIZED_ISPN_HOME=$(cd "$ISPN_HOME"; pwd)
  if [ "$RESOLVED_ISPN_HOME" != "$SANITIZED_ISPN_HOME" ]; then
    echo ""
    echo "   WARNING:  ISPN_HOME may be pointing to a different installation - unpredictable results may occur."
    echo ""
    echo "             ISPN_HOME: $ISPN_HOME"
    echo ""
    sleep 2s
  fi
fi
export ISPN_HOME

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    BASEPROGNAME=$(basename "$PROGNAME" .sh)
    RUN_CONF="$DIRNAME/$BASEPROGNAME.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF"
fi

JAVA_OPTS="$JAVA_OPTS_EXTRA $JAVA_OPTS"

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT=$(echo "$JAVA_OPTS" | $GREP "\-agentlib:jdwp")
    if [ "x$DEBUG_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

# Enable JMX authenticator if needed
if [ "$JMX_REMOTING" = "true" ]; then
    JMX_OPT=$(echo "$JAVA_OPTS" | $GREP "\-Dcom.sun.management.jmxremote")
    if [ "x$JMX_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT -Djava.security.auth.login.config=$DIRNAME/server-jaas.config -Dcom.sun.management.jmxremote.login.config=ServerJMXConfig -Dcom.sun.management.jmxremote.ssl=false"
    else
        echo "JMX already enabled in JAVA_OPTS, ignoring --jmx argument"
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

if $linux; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $ARGUMENTS $PROPERTIES"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=$(echo "$var" | tr -d "'")
       case $p in
         -Dinfinispan.server.root.path=*|--server-root=*)
              ISPN_ROOT_DIR=$(readlink -m "${p#*=}")
              ;;
         -Dinfinispan.server.log.path=*)
              ISPN_LOG_DIR=$(readlink -m "${p#*=}")
              ;;
         -Dinfinispan.server.config.path=*)
              ISPN_CONFIG_DIR=$(readlink -m "${p#*=}")
              ;;
       esac
    done
fi

if $solaris; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $ARGUMENTS $PROPERTIES"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=$(echo "$var" | tr -d "'")
       case $p in
         -Dinfinispan.server.root.path=*|--server-root=*)
             ISPN_ROOT_DIR=$(echo "$p" | awk -F= '{print $2}')
             ;;
         -Dinfinispan.server.log.path=*)
             ISPN_LOG_DIR=$(echo "$p" | awk -F= '{print $2}')
             ;;
         -Dinfinispan.server.config.path=*)
             ISPN_CONFIG_DIR=$(echo "$p" | awk -F= '{print $2}')
             ;;
      esac
    done
fi

# No readlink -m on BSD
if $darwin || $freebsd || $other ; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $ARGUMENTS $PROPERTIES"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=$(echo "$var" | tr -d "'")
       case $p in
         -Dinfinispan.server.root.path=*|--server-root=*)
              ISPN_ROOT_DIR=$(cd "${p#*=}" ; pwd -P)
              ;;
         -Dinfinispan.server.log.path=*)
              if [ -d "${p#*=}" ]; then
                ISPN_LOG_DIR=$(cd "${p#*=}" ; pwd -P)
             else
                #since the specified directory doesn't exist we don't validate it
                ISPN_LOG_DIR=${p#*=}
             fi
             ;;
         -Dinfinispan.server.config.path=*)
              ISPN_CONFIG_DIR=$(cd "${p#*=}" ; pwd -P)
              ;;
       esac
    done
fi

# determine the default base dir, if not set
if [ "x$ISPN_ROOT_DIR" = "x" ]; then
   ISPN_ROOT_DIR="$ISPN_HOME/server"
fi
# determine the default log dir, if not set
if [ "x$ISPN_LOG_DIR" = "x" ]; then
   ISPN_LOG_DIR="$ISPN_ROOT_DIR/log"
fi
# determine the default configuration dir, if not set
if [ "x$ISPN_CONFIG_DIR" = "x" ]; then
   ISPN_CONFIG_DIR="$ISPN_ROOT_DIR/conf"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    ISPN_HOME=$(cygpath --path --windows "$ISPN_HOME")
    JAVA_HOME=$(cygpath --path --windows "$JAVA_HOME")
    ISPN_MODULEPATH=$(cygpath --path --windows "$ISPN_MODULEPATH")
    ISPN_ROOT_DIR=$(cygpath --path --windows "$ISPN_ROOT_DIR")
    ISPN_LOG_DIR=$(cygpath --path --windows "$ISPN_LOG_DIR")
    ISPN_CONFIG_DIR=$(cygpath --path --windows "$ISPN_CONFIG_DIR")
fi

if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=$(echo $JAVA_OPTS | $GREP "\-d64")
    JVM_D32_OPTION=$(echo $JAVA_OPTS | $GREP "\-d32")

    # Check If server or client is specified
    SERVER_SET=$(echo $JAVA_OPTS | $GREP "\-server")
    CLIENT_SET=$(echo $JAVA_OPTS | $GREP "\-client")

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    if [ "x$CLIENT_SET" = "x" -a "x$SERVER_SET" = "x" ]; then
        # neither -client nor -server is specified
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    JAVA_VERSION=$(java -version 2>&1 | sed -n ';s/.* version "\([^\.\-]*\).*".*/\1/p;')
    if [ "$JAVA_VERSION" -ge 17 ]; then
        # Enable export for LDAP (needed for JDK 17+)
        PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS --add-exports java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED"
    fi

    if [ "$JAVA_VERSION" -ge 18 ]; then
        # Change back default value of java.security.manager (needed for JDK 18+)
        PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -Djava.security.manager=allow"
    fi

    if [ "$GC_LOG" = "true" ]; then
        # Enable rotating GC logs if the JVM supports it and GC logs are not already enabled
        mkdir -p "$ISPN_LOG_DIR"
        NO_GC_LOG_ROTATE=$(echo $JAVA_OPTS | $GREP "\-Xlog\:\?gc")
        if [ "x$NO_GC_LOG_ROTATE" = "x" ]; then
            # backup prior gc logs
            mv -f "$ISPN_LOG_DIR/gc.log" "$ISPN_LOG_DIR/backupgc.log" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR/gc.log.0" "$ISPN_LOG_DIR/backupgc.log.0" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR/gc.log.1" "$ISPN_LOG_DIR/backupgc.log.1" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR/gc.log.2" "$ISPN_LOG_DIR/backupgc.log.2" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR/gc.log.3" "$ISPN_LOG_DIR/backupgc.log.3" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR/gc.log.4" "$ISPN_LOG_DIR/backupgc.log.4" >/dev/null 2>&1
            mv -f "$ISPN_LOG_DIR"/gc.log.*.current "$ISPN_LOG_DIR/backupgc.log.current" >/dev/null 2>&1

            "$JAVA" -Xverbosegclog:"$ISPN_LOG_DIR/gc.log" -version > /dev/null 2>&1 && OPEN_J9_JDK=true || OPEN_J9_JDK=false
            if [ "$OPEN_J9_JDK" = "true" ]; then
                TMP_PARAM="-Xverbosegclog:\"$ISPN_LOG_DIR/gc.log\""
            else
                TMP_PARAM="-Xlog:gc*:file=\"$ISPN_LOG_DIR/gc.log\":time,uptimemillis:filecount=5,filesize=3M"
            fi
            eval "$JAVA" $JVM_OPTVERSION $TMP_PARAM -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS $TMP_PARAM"
            # Remove the gc.log file from the -version check
            rm -f "$ISPN_LOG_DIR/gc.log" >/dev/null 2>&1
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

CLASSPATH=
for JAR in "$ISPN_HOME/boot"/*.jar; do
    CLASSPATH="$CLASSPATH:$JAR"
done
