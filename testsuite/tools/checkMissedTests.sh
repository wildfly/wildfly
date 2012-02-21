#!/bin/bash


##  Determine this script's location ("Tools" home dir).
scriptPath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# For the case when called through a symlink
scriptPath=`readlink -f "$scriptPath"`
scriptDir=`dirname $scriptPath`

#############

MODULE=""

truncate -s 0 /tmp/AS.ts.tests.txt;

for NAME in `find $scriptDir/../$MODULE -name *TestCase.java` ; do echo $NAME | sed "s/\.java//" >>  /tmp/AS.ts.tests.txt;  done
for NAME in `find $scriptDir/../$MODULE -name *TestCase.xml`  ; do echo $NAME | sed "s/\.xml//"  >>  /tmp/AS.ts.tests.txt;  done

#sort /tmp/AS.ts.tests.txt > /tmp/AS.ts.tests.sort.txt

echo "## Tests which were not run:"
uniq -u /tmp/AS.ts.tests.txt



