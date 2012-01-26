Description
===========
 
Maven BOM files enhancing Jave EE 6 with deployment and test tooling. These files manage version of dependencies you 
want to use in your project, so you can pretty sure you always get a compatible stack.

Usage
=====

Depending whether you want to use Java EE 6 Web Profile or Java EE 6 Full Profile, add following into your pom.xml:

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.jboss.spec</groupId>
        		<artifactId>jboss-javaee-web-6.0-with-tools-</artifactId>
        		<version>1.0.0-SNAPSHOT</version>
        	</dependency>
		</dependencies>
	</dependencyManagement> 
	
Respectively, for the Java EE Full Profile:

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.jboss.spec</groupId>
        		<artifactId>jboss-javaee-6.0-with-tools-</artifactId>
        		<version>1.0.0-SNAPSHOT</version>
        	</dependency>
		</dependencies>
	</dependencyManagement> 

Later, you have to ensure you're running with correct versions of plugin. Add following snippet to ``<build>`` section of your pom.xml:

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

Use cases
=========

There are various use case, let's cover the most popular ones.

Deploying/undeploying your application to server
------------------------------------------------

To be able to easily deploy (or undeploy) your application from the application server, include following in the ``<build>`` section of your pom.xml file:
	
	<plugins>    
    	<plugin>
        	<groupId>org.jboss.as.plugins</groupId>
            <artifactId>jboss-as-maven-plugin</artifactId>
        </plugin>
    </plugins>
    
You'll be able to deploy your application via ``mvn package jboss-as:deploy``. See <https://github.com/jbossas/jboss-as-maven-plugin> for further information how to use the plugin.
 	
Testing your application with Arquillian
----------------------------------------

To able to test your application with Arquillian, you have decide which type container invocation you prefer. Arquillian allows you to choice 
between a managed invocation, where it controls startup and shutdown of the container and a remote invocation, which connects to an already running 
container. See <https://docs.jboss.org/author/display/ARQ/Container+varieties> for further details. If you are experienced Maven user, you can
obviously set up two distint profiles, each using one type of the container. 
 	
To select JBoss AS 7 managed container, following dependency has to be added into the ``<dependencies>`` section of your pom.xml file:
	
	<dependency>
    	<groupId>org.jboss.as</groupId>
        <artifactId>jboss-as-arquillian-container-managed</artifactId>
        <scope>test</scope>
    </dependency>
	
On the other hand, for JBoss AS 7 remote container, the dependency coordinates looks are:

	<dependency>
    	<groupId>org.jboss.as</groupId>
        <artifactId>jboss-as-arquillian-container-remote</artifactId>
        <scope>test</scope>
    </dependency>
    
Apart from setting a container, you need to add a testing framework (e.g. JUnit) and Arquillan bindings for it:

	<dependency>
    	<groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.jboss.arquillian.junit</groupId>
        <artifactId>arquillian-junit-container</artifactId>
        <scope>test</scope>
    </dependency>

*Note: Don't forget to set JBOSS_HOME environment variable so Arquillian will be able to find your container location.
If you want to experiment with Arquillian settings, you can find plenty of information at <http://arqpreview-alrubinger.rhcloud.com/>*

Testing your application with Arquillian Drone
----------------------------------------------

Arquillian Drone uses the very same setup as plain Arquillian. Arquillian Drone lets you choice between different Selenium bindings.
Here we cover Arquillian Ajocado, for further binding please follow <https://docs.jboss.org/author/display/ARQ/Drone>.

In order to use Arquillian with Arquillian Drone, include following dependency into your ``<dependencies>`` section:

	<dependency>
		<groupId>org.jboss.arquillian.ajocado</groupId>
		<artifactId>arquillian-ajocado-junit</artifactId>
		<scope>test</scope>
		<type>pom</type>
	</dependency>

*Note: Ajocado already contains a certified version of Selenium. Should you need to use a different version, for example to test you 
application in a very new browser compatible with the latest Selenium only, you can get more information how to do that at <https://community.jboss.org/wiki/SpecifyingSeleniumVersionInArquillianDrone>*  
     
