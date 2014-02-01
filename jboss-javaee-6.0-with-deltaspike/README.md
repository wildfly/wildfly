JBoss Java EE 6 with Deltaspike
===============================

This BOM builds on the Java EE full profile BOM, adding Deltaspike.
 
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.wildfly.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-deltaspike</artifactId>
               <version>8.0.0-SNAPSHOT</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
