#! /bin/bash
#set -x

##  This is testsuite/tools/jacoco/prepare.sh

PROGNAME=`basename $0`
DIRNAME=`dirname $0`
SCRIPT_DIR=`readlink -f $DIRNAME`

AS_DIR=`ls -d -1 $SCRIPT_DIR/../../../build/target/jboss-as-*` 


#wget http://ignum.dl.sourceforge.net/project/eclemma/07_JaCoCo/trunk/jacoco-0.5.6.201201122002.zip -O jacoco.zip
wget http://heanet.dl.sourceforge.net/project/eclemma/07_JaCoCo/0.5.5/jacoco-0.5.5.201112152213.zip -O jacoco.zip
unzip -q jacoco.zip -d tmp-jacoco 

##  Emma instrumentation.
if [ $SUCC == 0 ] ; then
  for i in `find $AS_DIR/modules/org/jboss/ -name '*.jar'`; do
    echo "============  $i"
    java -cp emma.jar emma instr -outmode overwrite -merge yes -instrpath $i;
  done
fi

mkdir -p $AS_DIR/modules/com/vladium/emma/main
cp emma.jar $AS_DIR/modules/com/vladium/emma/main/
cp $SCRIPT_DIR/module.xml $AS_DIR/modules/com/vladium/emma/main/
 
