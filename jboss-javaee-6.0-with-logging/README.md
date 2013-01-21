JBoss Java EE 6 with logging tools recommended by JBoss
========================================================

This BOM builds on the Java EE full profile BOM, adding the JBoss Logging Tools and Log4 framework. 
  
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.jboss.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-logging</artifactId>
               <version>1.0.4.CR7</version>
               <type>pom</type>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 
	