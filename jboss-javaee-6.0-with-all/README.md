JBoss Java EE 6 with ALL
===============================

This BOM builds on the Java EE full profile BOM, adding all JBoss BOMs.
 
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.jboss.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-all</artifactId>
               <version>1.0.4.CR8</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
