#! /bin/bash
#set -x

##  This is testsuite/tools/jacoco/prepare.sh

PROGNAME=`basename $0`
DIRNAME=`dirname $0`
SCRIPT_DIR=`readlink -f $DIRNAME`

AS_DIR=`ls -d -1 $SCRIPT_DIR/../../../build/target/jboss-as-*` 


##  -javaagent:/home/ondra/.m2/repository/org/jacoco/org.jacoco.agent/0.5.5.201112152213/org.jacoco.agent-0.5.5.201112152213-runtime.jar=destfile=${basedir}/target/jacoco.exec,append=true,includes=org.jboss.as.*,excludes=org.jboss.as.test*,output=file

mkdir -p target
#wget http://ignum.dl.sourceforge.net/project/eclemma/07_JaCoCo/trunk/jacoco-0.5.6.201201122002.zip -O jacoco.zip
wget http://heanet.dl.sourceforge.net/project/eclemma/07_JaCoCo/0.5.5/jacoco-0.5.5.201112152213.zip -O target/jacoco.zip
unzip -q target/jacoco.zip -d target/jacoco-dist 
#cp target/jacoco-dist/jacocoagent.jar 
