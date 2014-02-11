WildFly JBoss Java EE 7 with Resteasy
=============================

This BOM builds on the Java EE full profile BOM, adding Resteasy.
 
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.wildfly.bom</groupId>
               <artifactId>jboss-javaee-7.0-with-resteasy</artifactId>
               <version>8.0.0-SNAPSHOT</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
