#!/bin/sh
#
# JBoss domain control script
#
# chkconfig: - 80 20
# description: JBoss AS Domain
# processname: domain
# pidfile: /var/run/jboss-as/jboss-as-domain.pid
# config: /etc/jboss-as/jboss-as.conf

# Source function library.
. /etc/init.d/functions

# Load Java configuration.
[ -r /etc/java/java.conf ] && . /etc/java/java.conf
export JAVA_HOME

# Load JBoss AS init.d configuration.
if [ -z "$JBOSS_CONF" ]; then
  JBOSS_CONF="/etc/jboss-as/jboss-as.conf"
fi

[ -r "$JBOSS_CONF" ] && . "${JBOSS_CONF}"

# Set defaults.

if [ -z "$JBOSS_HOME" ]; then
  JBOSS_HOME=/usr/share/jboss-as
fi
export JBOSS_HOME

if [ -z "$JBOSS_PIDFILE" ]; then
  JBOSS_PIDFILE=/var/run/jboss-as/jboss-as-domain.pid
fi
export JBOSS_PIDFILE

if [ -z "$JBOSS_CONSOLE_LOG" ]; then
  JBOSS_CONSOLE_LOG=/var/log/jboss-as/console.log
fi

if [ -z "$STARTUP_WAIT" ]; then
  STARTUP_WAIT=30
fi

if [ -z "$SHUTDOWN_WAIT" ]; then
  SHUTDOWN_WAIT=30
fi

if [ -z "$JBOSS_DOMAIN_CONFIG" ]; then
  JBOSS_DOMAIN_CONFIG=domain.xml
fi

if [ -z "$JBOSS_HOST_CONFIG" ]; then
  JBOSS_HOST_CONFIG=host.xml
fi

JBOSS_SCRIPT=$JBOSS_HOME/bin/domain.sh

if [ -z "$PROG" ]; then
  PROG='jboss-as'
fi

CMD_PREFIX=''

if [ ! -z "$JBOSS_USER" ]; then
  if [ -x /etc/rc.d/init.d/functions ]; then
    CMD_PREFIX="daemon --user $JBOSS_USER"
  else
    CMD_PREFIX="su - $JBOSS_USER -c"
  fi
fi

start() {
  echo -n "Starting $PROG: "
  if [ -f $JBOSS_PIDFILE ]; then
    read ppid < $JBOSS_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      echo -n "$PROG is already running"
      failure
      echo
      return 1
    else
      rm -f $JBOSS_PIDFILE
    fi
  fi
  mkdir -p $(dirname $JBOSS_CONSOLE_LOG)
  cat /dev/null > $JBOSS_CONSOLE_LOG

  mkdir -p $(dirname $JBOSS_PIDFILE)
  chown $JBOSS_USER $(dirname $JBOSS_PIDFILE) || true
  #$CMD_PREFIX JBOSS_PIDFILE=$JBOSS_PIDFILE $JBOSS_SCRIPT 2>&1 >> $JBOSS_CONSOLE_LOG &
  #$CMD_PREFIX JBOSS_PIDFILE=$JBOSS_PIDFILE $JBOSS_SCRIPT &

  if [ ! -z "$JBOSS_USER" ]; then
    if [ -r /etc/rc.d/init.d/functions ]; then
      daemon --user $JBOSS_USER LAUNCH_JBOSS_IN_BACKGROUND=1 JBOSS_PIDFILE=$JBOSS_PIDFILE $JBOSS_SCRIPT --domain-config=$JBOSS_DOMAIN_CONFIG --host-config=$JBOSS_HOST_CONFIG 2>&1 >> $JBOSS_CONSOLE_LOG &
    else
      su - $JBOSS_USER -c "LAUNCH_JBOSS_IN_BACKGROUND=1 JBOSS_PIDFILE=$JBOSS_PIDFILE $JBOSS_SCRIPT --domain-config=$JBOSS_DOMAIN_CONFIG --host-config=$JBOSS_HOST_CONFIG" 2>&1 >> $JBOSS_CONSOLE_LOG &
    fi
  fi

  count=0
  launched=false

  until [ $count -gt $STARTUP_WAIT ]
  do
    grep 'JBoss AS.*started in' $JBOSS_CONSOLE_LOG > /dev/null
    if [ $? -eq 0 ] ; then
      launched=true
      break
    fi
    sleep 1
    let count=$count+1;
  done

  success
  echo
  return 0
}

stop() {
  echo -n $"Stopping $PROG: "
  count=0;

  if [ -f $JBOSS_PIDFILE ]; then
    read kpid < $JBOSS_PIDFILE
    let kwait=$SHUTDOWN_WAIT

    # Try issuing SIGTERM

    kill -15 $kpid
    until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
    do
      sleep 1
      let count=$count+1;
    done

    if [ $count -gt $kwait ]; then
      kill -9 $kpid
    fi
  fi
  rm -f $JBOSS_PIDFILE
  success
  echo
}

status() {
  if [ -f $JBOSS_PIDFILE ]; then
    read ppid < $JBOSS_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      echo "$PROG is running (pid $ppid)"
      return 0
    else
      echo "$PROG dead but pid file exists"
      return 1
    fi
  fi
  echo "$PROG is not running"
  return 3
}

case "$1" in
  start)
      start
      ;;
  stop)
      stop
      ;;
  restart)
      $0 stop
      $0 start
      ;;
  status)
      status
      ;;
  *)
      ## If no parameters are given, print which are avaiable.
      echo "Usage: $0 {start|stop|status|restart|reload}"
      exit 1
      ;;
esac
