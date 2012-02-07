#!/bin/bash


##  Determine this script's location ("Tools" home dir).
scriptPath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# For the case when called through a symlink
scriptPath=`readlink -f "$scriptPath"`
scriptDir=`dirname $scriptPath`

#############

EDITOR=${EDITOR:-less}

LOGS=`find $scriptDir/.. -name *$1*.xml -or -name *$1*-output.txt`

if [ "" == "$LOGS" ] ; then
  echo "No logs found.";
  exit 0;
fi;

$EDITOR $LOGS;


