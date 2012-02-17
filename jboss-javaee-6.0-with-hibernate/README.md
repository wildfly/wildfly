This BOM builds on the Java EE full profile BOM, adding Hibernate family projects (Hibernate ORM, Hibernate Search,
Hibernate Validator).

It also provides tool projects such as Hibernate JPA Model Gen and Hibernate Validator Annotation Processor.

Usage
=====

To use the BOM, import into your dependency management:

    <dependencyManagement>
        <dependencies>
	    <dependency>
	       <groupId>org.jboss.bom</groupId>
               <artifactId>jboss-javaee-6.0-with-hibernate</artifactId>
               <version>1.0.0-SNAPSHOT</version>
               <type>pom</scope>
               <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement> 

