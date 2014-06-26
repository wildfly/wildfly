#!/bin/sh
MAVEN_VERSION=3.2.1
MAVEN_URL=http://www.us.apache.org/dist/maven/maven-3/3.2.1/binaries/apache-maven-3.2.1-bin.zip

if [ -d tools ]; then
  #executed from root of WF install
  cd tools
fi

if [ -d maven ]; then
  echo "Maven already exists"
  version=`./maven/bin/mvn -version | grep "Apache Maven $MAVEN_VERSION"`
  if [ "$version" ]; then
    echo "Maven is correct version"
    exit
  fi
fi

rm -rf maven
curl $MAVEN_URL >maven.zip
unzip maven.zip
rm maven.zip
mv apache-maven* maven

