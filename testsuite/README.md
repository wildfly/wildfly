WildFly Testsuite
=================

This directory contains the WildFly testsuite in its new format, as described at https://docs.jboss.org/author/display/WFLY10/Testsuite

Some points to note:


1. Defined modules
------------------

The testsuite has the following maven modules:

* compat      - compatibility tests (require special dependencies)
* domain      - domain management integration tests (require special framework)
* integration - general integration tests


2. Defined directories and files
--------------------------------

   testsuite/pom.xml - this has a header describing the purpose of the pom and what should and should not go in it
   testsuite/integration/pom.xml - this has a header describing the purpose of the pom and what should and should not go in it

For each logical grouping of tests X (e.g. basic integration, clustering, compat, smoke, ...), we use the following locations:

   src/test/java/org/jboss/as/X - holds all sources and test-specific resources for the X tests
   src/test/resources/X - holds resources shared by X tests, which include resources for creating deployments
       as well as Arquillian configuration files

   src/test/resources/test-configs/ - holds all files to be copied into custom configurations
   src/test/scripts - holds ant build scripts for tasks which require execution of maven-antrun-plugin (e.g building configs)
   src/test/xslt - holds XSLT stylesheets for making changes to AS XML-based configuration files


3. Defined profiles
-------------------

maven build profiles are used to control:
* testsuite modules enabled
* test cases to be executed within a module

3.1 testsuite modules
---------------------

- for each module X, the profile is named X.module.profile and is enabled by -DX.module
- for example, integration.module.profile, enabled by -Dintegration.module, enables the integration module
- there are profiles for api, domain and integration

3.2 test cases to be executed
-----------------------------

- for each logical set of test cases X in module Y, the profile is named X.Y.tests.profile and is enabled by -DX.Y.tests
- e.g.,  the clustering tests are defined in profile clustering.integration.tests.profile, enabled by -Dclustering.integration.tests

- in the integration module, there are profiles for the following logical groups:
-- basic.integration.tests.profile
-- clustering.integration.tests.profile
-- smoke.integration.tests.profile

As time goes on, more logical groupings of tests into profiles will emerge. These are required when a set of
tests needs to execute with custom server builds/configurations, server startup parameters or client side parameters.

All of these are activated independently of each other, unlike the -P usage.


4. Executing tests from the command line
----------------------------------------

Tests are run either when running the build.sh and build.bat shell scripts in the main build directory,
or using integration-tests.sh / .bat. The first way also runs the unit tests.

Run the default module and its tests (-Dintegration.module -Dsmoke.integration.tests)

> ./build.sh clean test

Run a selected set of modules and tests

> ./build.sh clean test -Ddomain-tests -Dintegration.module -Dbasic.integration.tests


5. Building server configurations
---------------------------------

Whenever we fire a surefire execution, this causes Arquillian to start a server(s), execute tests and then shutdown
the server(s). Starting a server in multiple executions kills the logs of previous executions, and so it's recommended
to create a new named server configuration for each surefire execution.

When we build a server configuration, we need to carry out several steps:
* make a copy of the original ${jboss.dist}, the server under test
* add/edit some configuration files (e.g. parametrizing the test cases for IP addresses, default databases and the like)
* copy in/delete some jars

Building server configurations is achieved in two different ways, depending on requirements:

5.1 maven-resources-plugin based build
--------------------------------------

In the shared build of target/jbossas in testsuite/pom.xml, i'm creating the server configuration using
the maven-resources-plugin, but this requires two separate executions:
(i) one to copy the WildFly distribution to a new directory (in generate-test-resources)
(ii) one to overwrite / add some files from src/resources/test-configs to the distribution copy
(in process-test-resources)

This is effectively the way buildmagic used to build configurations. We have to include all of this stuff for every
configuration we build, which is a lot of lines of plugin configuration.

Parametrization is achieved by XSLT transforms (changeIPAddresses.xsl, changeDatabase.xsl, addPortOffset.xsl).

5.2 maven-ant-plugin based build
--------------------------------

For almost all other server configurations, maven-antrun-plugin is used, which is much more compact.

A single ant target is called to construct the build of the server, using one or more of:
- copying jars from target/jdbcDrivers
- using test-configs and ant filtering to copy in updated files into the new configuration
- using XSLT stylesheets to automatically perform common editing tasks on the server's XML configuration files
  (e.g. IP addresses changed with setIPAddresses.xsl, database conf. changed with changeDefaultdatabase.xsl, etc.)


5.3 Sharing build configurations
--------------------------------

The testsuite pom in testsuite/pom.xml contains a surefire execution for building a server
configuration under target/jboss-as. This is used as a default version of the configuration which is inherited by
all modules and is used by tests not requiring a specially modified server configuration.
Corresponds to the all config in buildmagic.

Configurations for tests which do require their own configuration are built individually in the profile in question.
For example, clustering.integration.tests.profile contains executions to build clustering-udp-0, clustering-udp-1.


6. Parametrization of testsuite runs
------------------------------------

The testsuite/pom.xml contains a properties section which should hold all globally defined properties required to run
the testsuite and their defaults (e.g. IP addresses, IP stack configs, database configs, etc).
These properties will then be inherited by all of the testsuite modules.

At present, resource filtering is in the main powered by XSLT scripts, to allow substitution of properties specified
on the command line, such as:

> mvn clean test -Dintegration.module -Dclustering.integration.tests -Dnode0=192.168.0.1 -Dnode1=192.168.0.2


7. Changing the database
------------------------

The prototype also contains a profile which shows how the default database for WildFly can be changed.

> mvn clean test -Dds=mysql51 <module targets> <test targets>

will run the tests using MySQL 5.1 instead of the default H2 database.


