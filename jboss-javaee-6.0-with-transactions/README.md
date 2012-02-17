JBoss AS includes a world class transaction manager. In order to access it's full capabilites, including for example XTS, WS-AT and WS-BA, you need to use the JBossTS APIs.
 
Usage
=====

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
	          <dependency>
	              <groupId>org.jboss.bom</groupId>
                <artifactId>jboss-javaee-6.0-with-transactions</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 

You'll need to take a look at the POM source in order to find the latest versions of plugins recommended.
