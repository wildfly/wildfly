#!/bin/sh
MAVEN_VERSION=3.2.2
MAVEN_URL=http://www.us.apache.org/dist/maven/maven-3/3.2.2/binaries/apache-maven-3.2.2-bin.zip

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

#
# work around for incompatibility of maven 3.2.2 with SunOS: ./tools/maven/bin/mvn: syntax error at line 86: `(' unexpected
#
case `uname -s` in
"SunOS")
	cp -p maven/bin/mvn maven/bin/mvn.original
	echo >> maven/bin/mvn.original
	cat maven/bin/mvn.original | sed "s/JAVA_HOME=\$(\/usr\/libexec\/java_home)/JAVA_HOME=\/usr\/libexec\/java_home/" > maven/bin/mvn
	;;
*)
	;;
esac
