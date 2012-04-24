

How to create Public JBoss AS API aggregated JavaDoc
====================================================

1) run ./extractPublicApiArtifactsList.sh
This will print out a list of artifacts in this format:

<include>com.h2database:h2</include>
<include>commons-configuration:commons-configuration</include>
<include>dom4j:dom4j</include>
<include>javax.activation:activation</include>
...

2) Put these includes into build/pom.xml to the "javadocDist" profile.

3) cd build; mvn clean javadoc:aggregate -PjavadocDist;
This will fail because of AS7-4557 - Javadoc tool fails on certain AS dependencies' sources.

4) Workaround: Find which artifacts cause this issue and remove them from the set of <include>'s.

5) When done, aggregated JavaDoc will be created in:
  target/apidocs
  target/jboss-as-build-7.1.2.Final-SNAPSHOT-javadoc.jar
  
6) Check that the final result contains all packages it should.
  
  
  
  