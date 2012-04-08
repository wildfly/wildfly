#!/bin/bash


##  Determine this script's location ("Tools" home dir).
scriptPath="$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"
# For the case when called through a symlink
scriptPath=`readlink -f "$scriptPath"`
scriptDir=`dirname $scriptPath`

#############

MODULE=$1

function main {
    truncate -s 0 /tmp/AS.ts.tests.txt;

    if [ "$MODULE" == "" ] ; then
        all;
    else
        module;
    fi

    echo "###   Tests which were not run:  ###"
    #sort /tmp/AS.ts.tests.txt > /tmp/AS.ts.tests.sort.txt
    sort /tmp/AS.ts.tests.txt | uniq -u | grep -v '###'
}

###  Functions  ##########################

function all {
    for NAME in `find testsuite -name '*TestCase.java' | sort` ; do echo $NAME | sed "s#.*src/test/java/##g" | sed "s#\./\|\.java##g" | sed "s#/#.#g"  >>  /tmp/AS.ts.tests.txt;  done
    echo "###"  >>  /tmp/AS.ts.tests.txt;
    for NAME in `find testsuite -name '*TestCase.xml' -or  -name '*TestCase-*.xml' | sort`  ; do echo $NAME | sed "s#.*surefire-reports/##g" | sed "s#.*TEST-##g" | sed 's#TestCase\(-.*\)\?\.xml#TestCase#'  >>  /tmp/AS.ts.tests.txt;  done
    # for NAME in `find . -name *TestCase.xml`  ; do echo $NAME | sed "s#.*TEST-\|\.xml##g";  done
}

function module {
    if cd $scriptDir/../$MODULE/src/test/java/ ; then
	for NAME in `find . -name '*TestCase.java' | sort` ; do echo $NAME | sed "s#\./\|\.java##g" | sed "s#/#.#g"  >>  /tmp/AS.ts.tests.txt;  done
    else
	echo "Can't find $scriptDir/../$MODULE/src/test/java/";
	exit 1;
    fi
    echo "###" >>  /tmp/AS.ts.tests.txt;

    echo "$scriptDir/../$MODULE/target/surefire-reports/";
    if cd $scriptDir/../$MODULE/target/surefire-reports/ ; then
	for NAME in `find . -name '*TestCase.xml' | sort`  ; do echo $NAME | sed "s#.*TEST-\|\.xml##g"  >>  /tmp/AS.ts.tests.txt;  done
    else
	echo "Can't find $scriptDir/../$MODULE/target/surefire-reports/";
	exit 1;
    fi
}

function usage {
    echo "    Usage:";
    echo "           $0 <path-to-module-relative-from-testsuite/>";
    exit 1;
}

#for NAME in `find $scriptDir/../$MODULE/src/test/java/           -name *TestCase.java` ; do echo $NAME | sed "s/\.java//" | sed "s#/#.#g" >>  /tmp/AS.ts.tests.txt;  done
#for NAME in `find $scriptDir/../$MODULE/target/surefire-reports/ -name *TestCase.xml`  ; do echo $NAME | sed "s/TEST-|\.xml//"  >>  /tmp/AS.ts.tests.txt;  done

main;