#!/bin/bash


function usage {
    echo
    echo "    Usage:  testsuite/tools/showSrc.sh <part-of-file-name>"
    echo
}


##  Determine this script's location ("Tools" home dir).
scriptPath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# For the case when called through a symlink
scriptPath=`readlink -f "$scriptPath"`
scriptDir=`dirname $scriptPath`

#############


if [ "$1" = "" ] ; then usage; exit 1; fi


##  Pick an IDE.
EDITOR=${EDITOR:-vi}

if [ "$IDE" = "" ] && which netbeans &>/dev/null; then IDE=netbeans; fi
if [ "$IDE" = "" ] && which eclipse  &>/dev/null; then IDE=eclipse;  fi
if [ "$IDE" = "" ]; then
    echo "  No \$IDE set, using \$EDITOR - $EDITOR."
    IDE=$EDITOR;
fi


FIND="find $scriptDir/.. -name \*$1\*.java";

##  Prevent killing the IDE with hundreds of files.
NUM=$( $FIND | wc -l )
echo $NUM
if [[ $NUM -ge 30 ]] ; then
    echo "  ERROR:  More than 30 files found, not passing to the IDE to prevent overload.";
    $FIND | head -60
    echo "..."
    exit 2;
fi

SOURCES=`$FIND`
if [ "" == "$SOURCES" ] ; then
    echo "No sources found.";
    exit 0;
fi;


echo "  Opening $NUM files with $IDE."
$IDE $SOURCES;

