WildFly JBoss Java EE 7 with logging tools recommended by JBoss
========================================================

This BOM builds on the Java EE full profile BOM, adding the JBoss Logging Tools and Log4 framework. 
  
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.wildfly.bom</groupId>
               <artifactId>jboss-javaee-7.0-with-logging</artifactId>
               <version>8.0.0-SNAPSHOT</version>
               <type>pom</type>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 
	