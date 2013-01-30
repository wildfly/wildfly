JBoss Java EE 6 with Resteasy
=============================

This BOM builds on the Java EE full profile BOM, adding Resteasy.
 
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.jboss.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-resteasy</artifactId>
               <version>1.0.4-SNAPSHOT</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
