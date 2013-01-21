JBoss BOMs
==========

The JBoss BOM's project provides Maven BOM files enhancing Jave EE 6 with deployment and test tooling. These files manage the version of the dependencies you use in your project, ensuring you always get a compatible stack.

Usage
-----

To use the BOM, import into your dependency management. For example, if you wanted "Java EE with Tools recommended by JBoss", use:

    <dependencyManagement>    
        <dependencies>
            <dependency>
                <groupId>org.jboss.spec</groupId>
                <artifactId>jboss-javaee-web-6.0-with-tools</artifactId>
                <version>1.0.4.CR7</version>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 
	
Unfortunately, Maven doesn't allow you to specify plugin versions this way. The readme for each BOM calls out any plugin versions you should use. For example, to use the plugins associated with "Java EE with Tools recommended by JBoss":

    <pluginManagement>
        <plugins>
            <!-- The Maven Surefire plugin tests your application. Here we ensure we are using a version compatible with Arquillian -->
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.10</version>
            </plugin>
            <!-- The JBoss AS plugin deploys your war to a local JBoss AS container -->
            <!-- To use, set the JBOSS_HOME environment variable and run:
                 mvn package jboss-as:deploy -->
            <plugin>
                <groupId>org.jboss.as.plugins</groupId>
                <artifactId>jboss-as-maven-plugin</artifactId>
                <version>7.1.1.Final</version>
            </plugin>
        </plugins>
    </pluginManagement>

You'll need to take a look at the POM source in order to find the latest versions of plugins recommended.

