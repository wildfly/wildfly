mkdir -p target

### Includes and excludes.
sh extractPublicApiArtifactsList.sh --batch > target/dependencySourceInclExcl.xml

### Groups.
sh extractPackageGroupsByModules.sh  > target/groups.xml

### Copy <includes>, exclude errorneous .jar's

cd target
wget http://repo1.maven.org/maven2/xalan/xalan/2.7.1/xalan-2.7.1.jar -O xalan-2.7.1.jar 
wget http://repo1.maven.org/maven2/xalan/serializer/2.7.1/serializer-2.7.1.jar -O serializer-2.7.1.jar 
cd ..

java -cp target/serializer-2.7.1.jar:target/xalan-2.7.1.jar org.apache.xalan.xslt.Process -IN pom.xml -OUT pom2.xml -XSL pomSetIncludesAndGroups.xsl

sed -i 's/1500m/2048m/' pom2.xml

#failOnError false, so that javadoc continue even with some error
sed -i 's|<debug>true</debug>|<failOnError>false</failOnError><debug>true</debug>|' pom2.xml

