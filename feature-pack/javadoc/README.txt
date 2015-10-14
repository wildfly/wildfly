

How to create Public JBoss AS API aggregated JavaDoc
====================================================

1) Rebuild AS to get the modules into <AS>/build/target .

2) run ./extractPublicApiArtifactsList.sh
This will print out a list of artifacts in this format:

<include>com.h2database:h2</include>
<include>dom4j:dom4j</include>
<include>javax.activation:activation</include>
...

3) Put these includes into build/pom.xml to the "javadocDist" profile.

4) Run extractPackageGroupsByModules.sh
This will print out a list of javadoc groups in XML format:
    <group>
        <title>Module org.jboss.com.sun.httpserver</title>
        <packages>org.jboss.com.sun.net.httpserver:org.jboss.com.sun.net.httpserver.spi:org.jboss.sun.net.httpserver</packages>
    </group>
    <group>
        <title>Module org.jboss.common-beans</title>
        <packages>org.jboss.common.beans.property</packages>
    </group>

    Put these group definitions into build/pom.xml to the "javadocDist" profile.

5) cd <AS>/build;
   mvn javadoc:aggregate -PjavadocDist -Djavadoc.branding='JBoss Enterprise Application Platform 6.0.0.GA';
   ("javadoc.branding" will be used for page titles, headers, footers etc. Default is "JBoss Application Server public API - ${version}".)

This may fail because of AS7-4557 - Javadoc tool fails on certain AS dependencies' sources.
Workaround: Find which artifacts cause this issue and remove them from the set of <include>'s.

6) Another Javadoc bug is AS7-4719: MissingResourceException, key doclet.Same_package_name_used

Workaround: Find which groups cause this issue and remove the affected packages from their <packages>.
This is done by extractPackageGroupsByModules.sh but might happen when edited manually.
TattleTale duplicated classes report may help with this.

7) When done, aggregated JavaDoc will be created in:
  target/apidocs
  target/jboss-as-build-<version>-javadoc.jar

8) Check that the final result contains all packages it should.




