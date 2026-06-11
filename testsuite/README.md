# WildFly Testsuite


This directory contains the WildFly testsuite. This document describes how to execute the testsuite.

## Testsuite structure 

### Testsuite modules

The testsuite has the following maven modules that provide test classes:

| Module                                                                                                                                                          | Description                                                                                                                                        |
|----|----------------------------------------------------------------------------------------------------------------------------------------------------| 
| domain | Domain management integration tests (require special framework)                                                                                    | 
| galleon | Specialized tests of managing WildFly using the Galleon CLI                                                                                        |
| integration | General integration tests                                                                                                                          |
| layers | Validation of galleon layers provided by the `wildfly-ee` feature-pack.                                                                            |
| layers-expansion | Validation of galleon layers provided by the `wildfly` and `wildfly-preview` feature-packs.                                                        |
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
| test-feature-pack         | Feature-pack, based on `wildfly-ee`, that test modules can provision.                                                                                                                                             |
| test-feature-pack-preview | Feature-pack, based on `wildfly-preview`, that test modules can provision.                                                                                                                                        |
| test-product-conf         | Produces an artifact used by an integration/manualmode test.                                                                                                                                                      |

### Parent poms

  * `testsuite/pom.xml` - this has a header describing the purpose of the pom and what should and should not go in it
  * `testsuite/integration/pom.xml` - this has a header describing the purpose of the pom and what should and should not go in it

### Directory structure within a testsuite module
For each logical grouping of tests X (e.g. basic integration, clustering, smoke, ...), we use the following locations:

* `src/test/java/org/jboss/as/X` - holds all sources and test-specific resources for the X tests. For newer content the `src/test/java/org/wildfly/X` directory is used. (_If for a given `X` there is already a `org/jboss/as/X` directory, for new content please do not create a new `src/test/java/org/wildfly/X` directory; use the existing directory._)
* `src/test/resources/X` - holds resources shared by X tests, which include resources for creating deployments as well as Arquillian configuration files


## Controlling testsuite execution via profiles


Maven build profiles are used to control:
* the server variant being tested
* testsuite modules enabled
* test cases to be executed within a module
* behavior of plugins used to control the tests, including how the server being tested is provisioned and launched.

### Controlling the server variant being tested

The WildFly build produces four different feature-packs that can be in five different combinations to provision servers. Use -D to activate a profile to control what variant is tested and what dependencies are used to build test deployments and on the test client side.

**Note:** If none of the profiles listed below is specifically activated, the testsuite will execute as if the **latest-ee-full-server-tests** profile was activated.

Some testsuite modules or execution profiles provision a server using the `wildfly-maven-plugin` before running tests; others use pre-built installations created by the main build. The following table lists the different profiles that can be used and the feature-packs each uses for `wildfly-maven-plugin` provisioning and the location of the pre-built server that will be used.

| Profile                                                        | Feature-packs used                 | Pre-built server used               |
|----------------------------------------------------------------|------------------------------------|-------------------------------------|
| **latest-ee-only-server-tests**                                | `wildfly-ee`                       | `ee-build`                          |
| **latest-ee-full-server-tests**                                | `wildfly`                          | `build`                             |
| **legacy-ee-only-server-tests** AND **legacy-ee-tests**        | `wildfly-ee-10`                    | `legacy/ee-feature-pack/ee-build`   |
| **legacy-ee-full-server-tests** AND **legacy-ee-tests**        | `wildfly-ee-10` + `wildfly`        | `legacy/ee-feature-pack/build`      |
| **preview-server-tests**                                       | `wildfly-preview`                  | `preview/build`                     |

**Important:** When the `legacy-ee-only-server-tests` profile or the `legacy-ee-full-server-tests` profile is activated, the `legacy-ee-tests` profile must also be activated, at least if the `testsuite/integration/basic` module is executed. 
This is an unfortunate necessity due to the Maven 3 limitation of only allowing a single property to deactivate a profile. 
The `legacy-ee-tests` profile deactivates behavior that we want enabled by default.

#### Using an externally provisioned server

For testsuites modules/profile that don't logically require `wildfly-maven-plugin` provisioning, an externally provided pre-built server can be used. Use the **jboss.dist** or **jboss.inst** system property with a value pointing to the filesystem location of the pre-built server.

**Note:** Even if an externally provided pre-built server is used, unless the default behavior is wanted one of the profiles in the table above needs to be activated to tell the testsuite what dependencies should be used for test deployments and by the test client.

#### Overriding dependency versions with WildFly Channels

If, instead of using servers provisioned using dependency versions defined in the WildFly pom, you wish to use versions defined in external WildFly Channels manifests, use the **external.wildfly.channels** system property provide the locations of the channel manifests.

### Controlling which modules are enabled

There are three main groupings of tests, each of which is enabled via a profile

| Profile                    | Purpose                                                                                            | Notes                                                                                                                   |
|----------------------------|----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **base-server-tests**      | Tests functionality provided by the `wildfly-ee` feature-pack and the `wildfly-ee-10` feature-pack | Enabled by default. Disabled if `-Dpreview-server-tests` is used. Otherwise, to disable use `-P-base-server-tests`      |
| **expansion-server-tests** | Tests functionality provided by the `wildfly` feature-pack                                         | Enabled by default. Disabled if `-Dpreview-server-tests` is used. Otherwise, to disable use `-P-expansion-server-tests` |
| **preview-server-tests**   | Tests functionality provided by the `wildfly-preview` feature-pack                                 | Disabled by default. Use `-Dpreview-server-tests` to activate.                                                          |

#### The base server tests

The base server tests cover functionality provided by the `wildfly-ee` and `wildfly-ee-10` feature-packs.

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

See also "Profiles for specific testsuite modules" below for how to activate execution of specific modules.

#### The expansion server tests

The expansion server tests cover functionality provided by the `wildfly` feature-pack.

When the `expansion-server-tests` profile executes, by default no module containing tests executes. Executing test modules requires enabling one or more other profiles.

There are a number of other profiles that can be activated to control how tests are executed and which modules run. The following table shows the activation of these profiles and what they do.

| Activation  | Effect                                                                                                                                                                                                             |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-DallTests` | Runs *nearly* all of the modules that test base server functionality. Tests unslimmed traditional installations.                                                                                                   |
| `-Dts.bootable` | Runs all of the modules that are able to test bootable jars, configured to use the wildfly-maven-plugin to produce bootable jars.                                                                                  |
| `-Dts.layers`   | Runs all of the modules that are able to test slimmed servers, configured to use the wildfly-maven-plugin to produce various slimmed servers and test those, instead of testing unslimmed installations.           |
|`-Dts.microprofile`| Runs the `testsuite/integration/microprofile` and `testsuite/integration/microprofile-tck` modules                                                                                                                 |
|`-Dts.standalone.microprofile`| Runs a number of modules, including some that only test base server functionality, but using servers provisioned from the `wildfly` feature-pack and running the `standalone-microprofile[-ha].xml` configuration. |

#### The preview server tests

The base server tests cover functionality provided by the `wildfly-preview` feature-pack.

When the `preview-server-tests` profile executes, by default the `testsuite/integation/web` and `testsuite/integration/smoke` modules execute against an unslimmed, traditional (i.e. non-bootable-jar) installation.

There are a number of other profiles that can be activated to control how tests are executed and which modules run. The following table shows the activation of these profiles and what they do.

| Activation              | Effect |
|-------------------------|----|
| `-Dts.preview`          |Runs *nearly* all of the modules that test base server functionality. Tests unslimmed traditional installations. |
| `-Dts.bootable.preview` |Runs all of the modules that are able to test bootable jars, configured to use the wildfly-maven-plugin to produce bootable jars.|
| `-Dts.noSmoke`  | Disables execution of the `testsuite/integration/web` and `testsuite/integration/smoke`|

#### Profiles for specific testsuite modules

For the `domain` and `integration` modules, there is a profile named `X.module.profile` that can be enabled by `-DX.module`

For example, `integration.module.profile`, enabled by `-Dintegration.module`, enables the integration module.

The integration module includes a number of child modules. For many of these, there is a profile named `X.integration.tests.profile` which is enabled by `-DX.integration.tests`
- e.g.,  the clustering tests are defined in profile `clustering.integration.tests.profile`, enabled by `-Dclustering.integration.tests`

Many individual modules can also be run by executing a profile by passing a property in the form `-Dts.X` where X is the name of the lowest level maven module that contains the tests. For example, `-Dts.domain` runs the tests in `testsuite/domain` while `-Dts.clustering` runs the tests in `testsuite/integration/clustering`.

## Executing tests from the command line using convenience scripts

The WildFly source code includes scripts to assist with building WildFly and running the testsuite.**Using these scripts is not required.** The scripts simply provide some convenience wrappers around directly calling `mvn`. Directly using `mvn` is fine and is commonly done.

Tests are run either when running the `build.sh` and `build.bat` shell scripts,
or by using `integration-tests.sh` or `integration-tests.bat`. All of these scripts are found in the root directory of the source tree. The first way runs the entire build, and thus runs the unit tests.

Run the default module and its tests (-Dintegration.module -Dsmoke.integration.tests)

> ./build.sh clean test

Run a selected set of modules and tests

> ./build.sh clean test -Ddomain-tests -Dintegration.module -Dbasic.integration.tests


## Building server configurations

Whenever we fire a surefire execution, this causes Arquillian to start a server(s), execute tests and then shutdown
the server(s). Starting a server in multiple executions kills the test logs of previous executions, and so it's recommended
to create a new named server configuration for each surefire execution.

When we build a server configuration, we need to carry out several steps:
* Provision the server installation to be tested
* Perhaps edit some configuration files (e.g. parametrizing the test cases for IP addresses, default databases and the like)
* Perhaps copy in some modules

Provisioning server installations is achieved in two different ways, depending on requirements:

### maven-resources-plugin based provisioning

In the root testsuite/pom.xml file there is a maven-resources-plugin execution `ts.copy-wildfly` that in the `process-test-resources` phase copies most of the content of a WildFly build to the current project's 'testsuite/wildfly' directory. The `modules` directory content is not copied, so, when using such an installation, the server's `module.path` must be configured to include the `modules` dir of the original WildFly build.

The original WildFly build is found in either the `build`, `ee-build`, `legacy/ee-feature-pack/build`, `legacy/ee-feature-pack/ee-build` or `preview/build` module, or in an externally provided location, depending on the configuration of the maven build.

A module or profile that does not want a server installation to be provisioned this way can disable this provisioning by setting the `phase` of the `ts.copy-wildfly` execution to `none`.


### Galleon based provisioning with the wildfly-maven-plugin

Here the wildfly-maven-plugin `provision` goal is used to provision a server using
Galleon. The feature-packs and layers to use are controlled by the configuration of each plugin execution.


### Customizing the management configuration of a provisioned server

The general recommendation for customizing the management configuration of a provisioned server is to implement a `ServerSetupTask` and configure the relevant test classes to use it via the `ServerSetup` annotation.

If using `ServerSetupTask` isn't suitable (e.g. if the configuration settings need to be present for all tests against the installation), then JBoss CLI `.cli` scripts can be added to the test module's source and executed before the test phase begins using the wildfly-maven-plugin's `execute` goal.

There is some legacy code that does configuration modification via ant and xslt. This is being replaced and no new uses of this technique should be added.

### Adding JBoss Modules modules to the provisioned server

The general recommendation for adding JBoss Modules modules that a test needs is to use the
`TestModule` utility class from within the relevant test class.

If this approach isn't suitable, the maven-resources-plugin can be used.

### Other server installation customization

Addition of other content to the server installation can be done using the `maven-resources-plugin`. The `testsuite/integration/pom.xml` includes an execution that adds properties files to the `standalone/configuration` dir that provide users, credentials and groups that integration tests often expect.


## Parameterization of testsuite runs

The `testsuite/pom.xml` contains a properties section which should hold all globally defined properties required to run
the testsuite and their defaults (e.g. IP addresses, IP stack configs, database configs, etc).
These properties will then be inherited by all of the testsuite modules.

This allows substitution of properties specified
on the command line, such as:

> mvn clean test -Dintegration.module -Dclustering.integration.tests -Dnode0=192.168.0.1 -Dnode1=192.168.0.2

## Changing the container images used in integration tests

In integration tests, we use the following images:

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
