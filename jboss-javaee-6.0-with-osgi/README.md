JBoss Java EE 6 with OSGI
=========================

This BOM builds on the Java EE full profile BOM, adding OSGI.
  
Usage
-----

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
            <dependency>
               <groupId>org.jboss.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-osgi</artifactId>
               <version>1.0.1.CR5</version>
               <type>pom</type>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 
	