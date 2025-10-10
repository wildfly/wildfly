WildFly Testsuite
=================

This directory contains the WildFly testsuite.

Some points to note:


1.0 Defined modules
------------------

The testsuite has the following maven modules that provide test classes:

| Module                                                                                                                                                          | Description                                                                                                                                        |
|----|----------------------------------------------------------------------------------------------------------------------------------------------------| 
| domain | Domain management integration tests (require special framework)                                                                                    | 
| galleon | Specialized tests of managing WildFly using the Galleon CLI                                                                                        |
| integration | General integration tests                                                                                                                          |
| layers | Validation of galleon layers provided by the `wildfly-ee` feature pack.                                                                            |
| layers-expansion | Validation of galleon layers provided by the `wildfly` and `wildfly-preview` feature packs.                                                        |
| mixed-domain | Domain mode tests where the primary host controller is running the version under test while the secondary host controller runs a previous version. | 
| preview | Integration tests that are specific to WildFly Preview                                                                                             |
| scripts | Test various shell scripts provided by WildFly                                                                                                     |                                                                                                                                                   
The testsuite also includes modules that help support testsuite execution but that do not provide tests:

| Module                    | Description                                                                                                                                                                                                       |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| aggregator-base           | Controls which test modules are executed when executing the `base-server-tests` profile                                                                                                                           |
| aggregator-expansion      | Controls which test modules are executed when executing the `expansion-server-tests` profile                                                                                                                      |
| aggregator-preview        | Controls which test modules are executed when executing the `preview-server-tests` profile                                                                                                                        |
| build-demander-base       | Ensures the server build required by the `base-server-tests` profile has been built, if the maven build wasn't started with the `-Djboss.home` property used to run the testsuite against an external build.      |
| build-demander-expansion  | Ensures the server build required by the `expansion-server-tests` profile has been built, if the maven build wasn't started with the `-Djboss.home` property used to run the testsuite against an external build. |
| build-demander-preview    | Ensures the server build required by the `preview-server-tests` profile has been built, if the maven build wasn't started with the `-Djboss.home` property used to run the testsuite against an external build.   |
| shared                    | Shared utility classes that test classes can use.                                                                                                                                                                 |
| test-feature-pack         | Feature pack, based on `wildfly-ee`, that test modules can provision.                                                                                                                                             |
| test-feature-pack-preview | Feature pack, based on `wildfly-preview`, that test modules can provision.                                                                                                                                        |
| test-product-conf         | Produces an artifact used by an integration/manualmode test.                                                                                                                                                      |

2.0 Defined directories and files
--------------------------------

  * testsuite/pom.xml - this has a header describing the purpose of the pom and what should and should not go in it
  * testsuite/integration/pom.xml - this has a header describing the purpose of the pom and what should and should not go in it

For each logical grouping of tests X (e.g. basic integration, clustering, smoke, ...), we use the following locations:

* `src/test/java/org/jboss/as/X` - holds all sources and test-specific resources for the X tests. For newer content the `src/test/java/org/wildfly/X` directory is used. (_If for a given `X` there is already a `org/jboss/as/X` directory, for new content please do not create a new `src/test/java/org/wildfly/X` directory; use the existing directory._)
* `src/test/resources/X` - holds resources shared by X tests, which include resources for creating deployments as well as Arquillian configuration files


3.0 Defined profiles
-------------------

Maven build profiles are used to control:
* testsuite modules enabled
* test cases to be executed within a module
* behavior of plugins used to control the tests, including how the server being tested is provisioned and launched.

3.1 Top level test profiles
---------------------------

There are three main groupings of tests, each of which is enabled via a profile

| Profile                    | Purpose                                                            | Notes                                                                                                                   |
|----------------------------|--------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **base-server-tests**      | Tests functionality provided by the `wildfly-ee` feature pack      | Enabled by default. Disabled if `-Dpreview-server-tests` is used. Otherwise, to disable use `-P-base-server-tests`      |
| **expansion-server-tests** | Tests functionality provided by the `wildfly` feature pack         | Enabled by default. Disabled if `-Dpreview-server-tests` is used. Otherwise, to disable use `-P-expansion-server-tests` |
| **preview-server-tests**   | Tests functionality provided by the `wildfly-preview` feature pack | Disabled by default. Use `-Dpreview-server-tests` to activate.                                                          |

3.2 The base server tests
----------

The base server tests cover functionality provided by the `wildfly-ee` feature pack.

When the `base-server-tests` profile executes, by default the `testsuite/integation/web` and `testsuite/integration/smoke` modules execute against an unslimmed, traditional (i.e. non-bootable-jar) installation.

There are a number of other profiles that can be activated to control how tests are executed and which modules run. The following table shows the activation of these profiles and what they do.

| Activation      | Value                                              | Effect                                                                                                                                                                                                   |
|-----------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-DallTests`    | N/A                                                | Runs *nearly* all of the modules that test base server functionality. Test unslimmed traditional installations.                                                                                          |
|`-Dts.basic`| N/A                                                |Runs the `testsuite/integration/basic` and `testsuite/integration/ws` modules.|
| `-Dts.bootable` | N/A                                                | Runs all of the modules that are able to test bootable jars, configured to use the wildfly-maven-plugin to produce bootable jars.                                                                        |
| `-Dts.layers`   | N/A                                                | Runs all of the modules that are able to test slimmed servers, configured to use the wildfly-maven-plugin to produce various slimmed servers and test those, instead of testing unslimmed installations. |
| `-Dts.noSmoke`  | N/A                                                |Disables execution of the `testsuite/integration/web` and `testsuite/integration/smoke`|
|`-Djboss.test.mixed.domain.dir`| Path to a directory containing older version zips. |Enables the mixed domain tests.|

See also 3.5 below for how to activate execution of specific modules.

While these tests are only about functionality provided by the `wildfly-ee` feature pack, that doesn't mean the server installations under test are provisioned only using that feature pack. It is possible to control what server build is used for those test executions that test the builds produced by the overall build, and it is possible to control what feature pack is used for test installations that are created by the feature pack. This is done via system properties passed on the command line:

|Property| Value                          | Effect                                                                                                                                                                                                                                                                                          |
|----|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|`-Djboss.home`| Path to a WildFly installation | If this property is set, the given path will be used as the location of the server installation to test, instead of using one created in the build. (**Note:** *This setting doesn't affect the servers that are directly provisioned by the testsuite itself using the wildfly-maven-plugin.*) |
|`testsuite.default.build.project.prefix`| <empty>                        | Use the output of the `build` module as the source of installations not provisioned by the testsuite itself. This is the default behavior when testing  WildFly.                                                                                                                                |
|`testsuite.default.build.project.prefix`| `ee-`                          | Use the output of the `ee-build` module as the source of installations not provisioned by the testsuite itself.  This is the default behavior when testing Red Hat JBoss EAP.                                                                                                                   |  
|`testsuite.ee.galleon.pack.artifactId`| `wildfly-galleon-pack`         | When the testsuite provisions installations, it should use the `wildfly` feature pack.  This is the default behavior when testing  WildFly.                                                                                                                                                     |
|`testsuite.ee.galleon.pack.artifactId`| `wildfly-ee-galleon-pack`      | When the testsuite provisions installations, it should use the `wildfly` feature pack. This is the default behavior when testing Red Hat JBoss EAP.                                                                                                                                             |
|`testsuite.ee.galleon.pack.artifactId`| `wildfly-preview-feature-pack` | When the testsuite provisions installations, it should use the `wildfly` feature pack. This should only be used in combination with `-Dts.layers`.                                                                                                                                              |

3.3 The expansion server tests
-----------

The expansion server tests cover functionality provided by the `wildfly` feature pack.

When the `expansion-server-tests` profile executes, by default no module containing tests executes. Executing test modules requires enabling one or more other profiles.

There are a number of other profiles that can be activated to control how tests are executed and which modules run. The following table shows the activation of these profiles and what they do.

| Activation  | Effect                                                                                                                                                                                                             |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-DallTests` | Runs *nearly* all of the modules that test base server functionality. Tests unslimmed traditional installations.                                                                                                   |
| `-Dts.bootable` | Runs all of the modules that are able to test bootable jars, configured to use the wildfly-maven-plugin to produce bootable jars.                                                                                  |
| `-Dts.layers`   | Runs all of the modules that are able to test slimmed servers, configured to use the wildfly-maven-plugin to produce various slimmed servers and test those, instead of testing unslimmed installations.           |
|`-Dts.microprofile`| Runs the `testsuite/integration/microprofile` and `testsuite/integration/microprofile-tck` modules                                                                                                                 |
|`-Dts.standalone.microprofile`| Runs a number of modules, including some that only test base server functionality, but using servers provisioned from the `wildfly` feature pack and running the `standalone-microprofile[-ha].xml` configuration. |

3.4 The preview server tests
-----------

The base server tests cover functionality provided by the `wildfly-preview` feature pack.

When the `preview-server-tests` profile executes, by default the `testsuite/integation/web` and `testsuite/integration/smoke` modules execute against an unslimmed, traditional (i.e. non-bootable-jar) installation.

There are a number of other profiles that can be activated to control how tests are executed and which modules run. The following table shows the activation of these profiles and what they do.

| Activation              | Effect |
|-------------------------|----|
| `-Dts.preview`          |Runs *nearly* all of the modules that test base server functionality. Tests unslimmed traditional installations. |
| `-Dts.bootable.preview` |Runs all of the modules that are able to test bootable jars, configured to use the wildfly-maven-plugin to produce bootable jars.|
| `-Dts.noSmoke`  | Disables execution of the `testsuite/integration/web` and `testsuite/integration/smoke`|

3.5 Profiles for specific testsuite modules
---------------------

For the `domain` and `integration` modules, there is a profile named `X.module.profile` that can be enabled by `-DX.module`

For example, `integration.module.profile`, enabled by `-Dintegration.module`, enables the integration module.

The integration module includes a number of child modules. For many of these, there is a profile named `X.integration.tests.profile` which is enabled by `-DX.integration.tests`
- e.g.,  the clustering tests are defined in profile `clustering.integration.tests.profile`, enabled by `-Dclustering.integration.tests`

Many individual modules can also be run by executing a profile by passing a property in the form `-Dts.X` where X is the name of the lowest level maven module that contains the tests. For example, `-Dts.domain` runs the tests in `testsuite/domain` while `-Dts.clustering` runs the tests in `testsuite/integration/clustering`.



4.0 Executing tests from the command line using convenience scripts
----------------------------------------

The WildFly source code includes scripts to assist with building WildFly and running the testsuite.**Using these scripts is not required.** The scripts simply provide some convenience wrappers around directly calling `mvn`. Directly using `mvn` is fine and is commonly done.

Tests are run either when running the `build.sh` and `build.bat` shell scripts,
or by using `integration-tests.sh` or `integration-tests.bat`. All of these scripts are found in the root directory of the source tree. The first way runs the entire build, and thus runs the unit tests.

Run the default module and its tests (-Dintegration.module -Dsmoke.integration.tests)

> ./build.sh clean test

Run a selected set of modules and tests

> ./build.sh clean test -Ddomain-tests -Dintegration.module -Dbasic.integration.tests


5.0 Building server configurations
---------------------------------

Whenever we fire a surefire execution, this causes Arquillian to start a server(s), execute tests and then shutdown
the server(s). Starting a server in multiple executions kills the test logs of previous executions, and so it's recommended
to create a new named server configuration for each surefire execution.

When we build a server configuration, we need to carry out several steps:
* Provision the server installation to be tested
* Perhaps edit some configuration files (e.g. parametrizing the test cases for IP addresses, default databases and the like)
* Perhaps copy in some modules

Provisioning server installations is achieved in two different ways, depending on requirements:

5.1 maven-resources-plugin based provisioning
--------------------------------------

In the root testsuite/pom.xml file there is a maven-resources-plugin execution `ts.copy-wildfly` that in the `process-test-resources` phase copies most of the content of a WildFly build to the current project's 'testsuite/wildfly' directory. The `modules` directory content is not copied, so, when using such an installation, the server's `module.path` must be configured to include the `modules` dir of the original WildFly build.

The original WildFly build is found in either the `build`, `ee-build` or `preview/build` module, or in an externally provided location, depending on the configuration of the maven build.

A module or profile that does not want a server installation to be provisioned this way can disable this provisioning by setting the `phase` of the `ts.copy-wildfly` execution to `none`.


5.2  Galleon based provisioning with the wildfly-maven-plugin
--------------------------------

Here the wildfly-maven-plugin `provision` goal is used to provision a server using
Galleon. The feature packs and layers to use are controlled by the configuration of each plugin execution.


5.3 Customizing the management configuration of a provisioned server
--------------------------------

The general recommendation for customizing the management configuration of a provisioned server is to implement a `ServerSetupTask` and configure the relevant test classes to use it via the `ServerSetup` annotation.

If using `ServerSetupTask` isn't suitable (e.g. if the configuration settings need to be present for all tests against the installation), then JBoss CLI `.cli` scripts can be added to the test module's source and executed before the test phase begins using the wildfly-maven-plugin's `execute` goal.

There is some legacy code that does configuration modification via ant and xslt. This is being replaced and no new uses of this technique should be added.

5.4 Adding JBoss Modules modules to the provisioned server
------------------------------------------

The general recommendation for adding JBoss Modules modules that a test needs is to use the
`TestModule` utility class from within the relevant test class.

If this approach isn't suitable, the maven-resources-plugin can be used.

5.5 Other server installation customization
------------------------------------------

Addition of other content to the server installation can be done using the maven-resources-plugin. The testsuite/integration/pom.xml includes an execution that adds properties files to the `standalone/configuration` dir that provide users, credentials and groups that integration tests often expect.


6.0 Parametrization of testsuite runs
------------------------------------

The testsuite/pom.xml contains a properties section which should hold all globally defined properties required to run
the testsuite and their defaults (e.g. IP addresses, IP stack configs, database configs, etc).
These properties will then be inherited by all of the testsuite modules.

This allows substitution of properties specified
on the command line, such as:

> mvn clean test -Dintegration.module -Dclustering.integration.tests -Dnode0=192.168.0.1 -Dnode1=192.168.0.2


7.0 Changing the database
------------------------

The prototype also contains a profile which shows how the default database for WildFly can be changed.

> mvn clean test -Dds=mysql51 <module targets> <test targets>

will run the tests using MySQL 5.1 instead of the default H2 database.

8.0 Changing the container images used in integration tests
------------------------

In integrations tests, we use the following images:

* otel/opentelemetry-collector
* docker.elastic.co/elasticsearch/elasticsearch
* apache/james
* apache/kafka-native
* quay.io/arkmq-org/activemq-artemis-broker
* quay.io/keycloak/keycloak

It is possible to override the default images and versions by using:

* System properties, e.g. `-Dtestsuite.kafka-native.image=apache/kafka-native:3.9.0`
* Environment variables, e.g. `AS_TS_KAFKA_NATIVE_IMAGE=apache/kafka-native:3.9.0`
* Properties file specified via `testsuite.config.properties` system property or `TESTSUITE_CONFIG_PROPERTIES` environment variable, e.g. `-Dtestsuite.config.properties=/path/to/testsuite-config.properties` or `TESTSUITE_CONFIG_PROPERTIES=/path/to/testsuite-config.properties`; the properties file format has to be the same as for System properties e.g.:
     ```properties
     testsuite.opentelemetry-collector.image=otel/opentelemetry-collector:0.115.1
     testsuite.elasticsearch.image=docker.elastic.co/elasticsearch/elasticsearch:8.15.4
     testsuite.mailserver.image=apache/james:demo-3.8.2
     testsuite.kafka-native.image=apache/kafka-native:3.8.0
     testsuite.activemq-artemis-broker.image=quay.io/arkmq-org/activemq-artemis-broker:artemis.2.42.0
     testsuite.keycloak.image=quay.io/keycloak/keycloak:24.0.2
     ```
