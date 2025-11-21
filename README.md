<p align="center">
  <a href="https://wildfly.org">
      <img src="logo/wildfly_logo.svg" alt="wildfly logo" title="WildFly" width="600"/>
  </a>
</p>

# WildFly Application Server
https://wildfly.org

* Fast Startup
* Small Footprint
* Modular Design
* Unified Configuration and Management

And of course Jakarta EE and MicroProfile!

# Documentation

* https://docs.wildfly.org/

# Building

Prerequisites:

* JDK 17 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`
* On *nix systems, make sure that the maximum number of open files for the user running the build is at least 4096
  (check `ulimit -n`) or more, depending on what other i/o intensive processes the user is running.

To build with your own Maven installation:

    mvn install

Alternatively, you can use the Maven Wrapper script that downloads and installs (if necessary) the required Maven version to
`~/.m2/wrapper` and runs it from there. On Linux, run

    ./mvnw install

On Windows

    mvnw install

# Starting and Stopping WildFly

Change to the bin directory after a successful build

    $ cd build/target/wildfly-\[version\]/bin

Start the server in domain mode

    ./domain.sh

Start the server in standalone mode

    ./standalone.sh

To stop the server, press Ctrl + C, or use the admin console

    ./jboss-cli.sh --connect command=:shutdown

Check 'Getting Started Guide' in the WildFly documentation for more information about how to start and stop WildFly.

# `build` vs. `dist` directories

After running `mvn install`, WildFly will be available in two distinct directories, `build` and `dist`.

* The `build` directory contains a build of WildFly that is based on Maven artifact resolution for module configuration
* The `dist` directory, on the other hand, contains a full distributable build of WildFly

Using the `build` directory makes iterating with subsystem or module development easier since there is no need to rebuild the whole of WildFly or copy JAR files around on every change.

The `dist` directory is better suited when a full build of WildFly is needed for development or test purposes.

# Running the Testsuite

For basic smoke tests, simply: `mvn test`

To run all the tests

    mvn install -DallTests

The testsuite module contains several submodules which can be run individually as needed to speed up development.
Refer to our documentation section that describes the testsuite in details.

https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_testsuite/WildFly_Integration_Testsuite_User_Guide.adoc

[//]: # ( TODO Replace this link with published version of the document section once we do publish it with a permalink)

# Contributing

Please see the instructions available in the [contributing guide](CONTRIBUTING.md).

# License

* [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
