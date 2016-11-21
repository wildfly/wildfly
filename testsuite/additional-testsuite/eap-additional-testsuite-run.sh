#!/bin/bash
cd ../../tools
./download-maven.sh
cd ..

export M2_HOME=$PWD/tools/maven

cd $PWD/dist/target/wildfly-*

export JBOSS_FOLDER=$PWD
export JBOSS_VERSION=$(echo $JBOSS_FOLDER | cut -d '-' -f 2,3)

cd ../../../testsuite/additional-testsuite/eap-additional-testsuite

if [ -d "/store/repository" ]
then
  M2_REPO=/store/repository
  ./../../../tools/maven/bin/mvn clean install -Dwildfly -Dwildfly-jdk8 -Dserver-integration -Dmaven.repo.local=$M2_REPO > ../output.txt
else
  mvn clean install -Dwildfly -Dwildfly-jdk8 -Dserver-integration > ../output.txt
fi

echo $M2_REPO

echo "$(cat ../output.txt)"

if [ -f "../errors.txt" ]
then
  rm ../errors.txt
fi

if [[ $(cat ../output.txt) == *"ERROR"* ]]
then
    echo  >> ../errors.txt
    (echo "Eap Additional Testsuite was completed with errors ...") >> ../errors.txt
    echo  >> ../errors.txt
    (echo "BUILD ERRORS ...") >> ../errors.txt
    (grep 'ERROR' ../output.txt) >> ../errors.txt
    echo  >> ../errors.txt
    i=0
    while read line
    do 
        [ -z "$line" ] && break
        if [ $i -eq 0 ]
	then
	  (echo "TEST ERRORS ...") >> ../errors.txt
	fi
        echo "$line" >> ../errors.txt
        i=1
    done < <(grep -A10 'Tests run.*Failures.*Errors.* Skipped.*Time elapsed.*FAILURE' ../output.txt)
    echo  >> ../errors.txt
    echo "Eap Additional Testsuite was completed with errors ..."
else
    echo "Eap Additional Testsuite was completed successfully ..."
fi


