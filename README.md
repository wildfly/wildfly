Maven BOM files enhancing Jave EE 6 with deployment and test tooling. These files manage the version of the dependencies 
you use in your project, ensuring you always get a compatible stack.

Usage
=====

To use the BOM, import into your dependency management. For example, if you wanted "Java EE with Tools recommended by JBoss", use:

    <dependencyManagement>
        <dependencies>
	    <dependency>
	       <groupId>org.jboss.spec</groupId>
               <artifactId>jboss-javaee-web-6.0-with-tools-</artifactId>
               <version>7.0.2.CR1</version>
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
                <version>7.1.0.CR1b</version>
            </plugin>
        </plugins>
    </pluginManagement>

You'll need to take a look at the POM source in order to find the latest versions of plugins recommended.

Use cases
=========

A number of BOMs are provided, each of which address a specific use case. If your usage falls into a number of use cases, simply use all of the relevant BOMs.

jboss-javaee-6.0-with-tools: Java EE with Tools recommended by JBoss
--------------------------------------------------------------------

Java EE lacks any testing APIs, and for this reason JBoss developed the Arquillian project, along with it's various component projects, such as Arquillian Drone, and the sister project Shrinkwrap. This BOM builds on the Java EE full profile BOM, adding Arquillian to the mix. It also provides a version of JUnit and TestNG recommended for use with Arquillian.
 
Furthermore, this BOM adds the JBoss AS Maven deployment plugin. EAP 6's recommended mode of deployment is via the management APIs, and the Maven plugin is the recommended way to do this, if the customer is using Maven for building.
 
