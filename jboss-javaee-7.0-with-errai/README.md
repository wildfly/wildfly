JBoss Java EE 7 with Errai
==========================

This BOM builds on the Java EE full profile BOM, adding the Errai framework and the Google Web Toolkit plus its Maven plugin.
 
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.wildfly.bom</groupId>
               <artifactId>jboss-javaee-7.0-with-errai</artifactId>
               <version>8.0.0-SNAPSHOT</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
