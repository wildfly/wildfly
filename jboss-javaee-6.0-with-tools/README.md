Java EE lacks any testing APIs, and for this reason JBoss developed the Arquillian project, along with it's various component projects, such as Arquillian Drone, and the sister project Shrinkwrap. This BOM builds on the Java EE full profile BOM, adding Arquillian to the mix. It also provides a version of JUnit and TestNG recommended for use with Arquillian.
 
Furthermore, this BOM adds the JBoss AS Maven deployment plugin. EAP 6's recommended mode of deployment is via the management APIs, and the Maven plugin is the recommended way to do this, if the customer is using Maven for building.
 
Usage
=====

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
	    	<dependency>
	       		<groupId>org.jboss.bom</groupId>
               	<artifactId>jboss-javaee-6.0-with-tools</artifactId>
               	<version>1.0.0.M2</version>
               	<scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 
	
Unfortunately, Maven doesn't allow you to specify plugin versions this way. To use the plugins associated with "Java EE with Tools recommended by JBoss" BOM, add:

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
                <version>7.1.0.Final</version>
            </plugin>
        </plugins>
    </pluginManagement>

You'll need to take a look at the POM source in order to find the latest versions of plugins recommended.


Deploying/undeploying your application to server
------------------------------------------------

To be able to easily deploy (or undeploy) your application from the application server, include following in the ``<build>`` section of your pom.xml file:
	
    <plugins>    
        <plugin>
            <groupId>org.jboss.as.plugins</groupId>
            <artifactId>jboss-as-maven-plugin</artifactId>
        </plugin>
    </plugins>
    
You'll be able to deploy your application via `mvn package jboss-as:deploy`. See <https://github.com/jbossas/jboss-as-maven-plugin> for further information how to use the plugin.
	
Testing your application with Arquillian
----------------------------------------

To able to test your application with Arquillian, you have decide which type container you prefer. Arquillian allows you to choose 
between a managed invocation, where it controls startup and shutdown of the container and a remote invocation, which connects to a running instance of JBoss AS.
See <https://docs.jboss.org/author/display/ARQ/Container+varieties> for further details. You may wish to set up two distint profiles, each using one type of
the container.
 	
To select JBoss AS 7 managed container, following dependency has to be added into the `<dependencies>` section of your pom.xml file:
	
    <dependency>
        <groupId>org.jboss.as</groupId>
        <artifactId>jboss-as-arquillian-container-managed</artifactId>
        <scope>test</scope>
    </dependency>
	
Or for JBoss AS 7 remote container:

    <dependency>
        <groupId>org.jboss.as</groupId>
        <artifactId>jboss-as-arquillian-container-remote</artifactId>
        <scope>test</scope>
    </dependency>
    
Apart from setting a container, you need to choose a testing framework (e.g. JUnit) and add the Arquillan bindings for it:

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

Arquillian allows you to setup two distinct protocols for communication between Arquillian and application server, a Servlet
based and a JMX based one. In order to have a protocol activated, you need to add its artifact into `<dependencies>` section and configure
it in `arquillian.xml` file. We recommend you to use the Servlet 3.0 protocol. To set up it, add following dependency:

    <dependency>
        <groupId>org.jboss.arquillian.protocol</groupId>
        <artifactId>arquillian-protocol-servlet</artifactId>
        <scope>test</scope>
    </dependency>

Arquillian configuration file (`arquillian.xml`) is located by default in `src/test/resources` directory. You need to setup
Servlet protocol as default:

    <?xml version="1.0" encoding="UTF-8"?>
    <arquillian xmlns="http://jboss.org/schema/arquillian"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://jboss.org/schema/arquillian
        http://jboss.org/schema/arquillian/arquillian_1_0.xsd">

        <!-- Uncomment to have test archives exported to the file system for inspection -->
        <!--    <engine>  -->
        <!--       <property name="deploymentExportPath">target/</property>  -->
        <!--    </engine> -->

        <!-- Force the use of the Servlet 3.0 protocol with all containers, as it is the most mature -->
        <defaultProtocol type="Servlet 3.0" />

        <!-- Example configuration for a managed/remote JBoss AS 7 instance -->
        <container qualifier="jboss" default="true">
        <!-- If you want to use the JBOSS_HOME environment variable, just delete the jbossHome property -->
        <!--<configuration>-->
        <!--<property name="jbossHome">/path/to/jboss/as</property>-->
        <!--</configuration>-->
        </container>
    </arquillian>


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

*Note: Ajocado already contains a certified version of Selenium. Should you need to use a different version (for example to test your 
application in a newer very new browser, compatible with the latest Selenium version only), you can get more information how to do that
at <https://community.jboss.org/wiki/SpecifyingSeleniumVersionInArquillianDrone>*  

