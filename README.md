WildFly Application Server
========================
http://wildfly.org

* Fast Startup
* Small Footprint
* Modular Design
* Unified Configuration and Management

And of course Java EE!

Building
-------------------

Prerequisites:

* JDK 8 or newer - check `java -version`
* Maven 3.3.1 or newer - check `mvn -v`
* On *nix systems, make sure that the maximum number of open files for the user running the build is at least 4096
  (check `ulimit -n`) or more, depending on what other i/o intensive processes the user is running.

To build with your own Maven installation:

    mvn install

Alternatively, you can use the Maven Wrapper script that downloads and installs (if necessary) the required Maven version to
`~/.m2/wrapper` and runs it from there. On Linux, run

    ./mvnw install

On Windows

    mvnw install


Starting and Stopping WildFly
------------------------------------------
Change to the bin directory after a successful build

$ cd build/target/wildfly-\[version\]/bin

Start the server in domain mode

    ./domain.sh

Start the server in standalone mode

    ./standalone.sh

To stop the server, press Ctrl + C, or use the admin console

    ./jboss-cli.sh --connect command=:shutdown

More information: https://docs.jboss.org/author/display/WFLY10/Getting+Started+Guide

Contributing
------------------
https://developer.jboss.org/wiki/HackingOnWildFly

Build vs. Dist directories
--------------------------

After running `mvn install`, WildFly will be available in two distinct directories, `build` and `dist`.

* The `build` directory contains a build of WildFly that is based on Maven artifact resolution for module configuration
* The `dist` directory, on the other hand, contains a full distributable build of WildFly

Using the `build` directory makes iterating with subsystem or module development easier since there is no need to rebuild the whole of WildFly or copy JAR files around on every change.

The `dist` directory is better suited when a full build of WildFly is needed for development or test purposes.

Running the Testsuite
--------------------
The testsuite module contains several submodules including the following:

* "smoke" -- core tests that should be run as part of every build of the AS. Failures here will fail the build.
* "api" -- tests of features that involve end user use of the public JBoss AS 8 API. Should be run with no failures before any major commits.
* "cluster" -- tests of the WildFly HA clustering features. Should be run with no failures before any major commits.
* "domain" -- tests of the domain management features. Should be run with no failures before any major commits.
* "integration" -- tests of a WildFly standalone server's internals. Should be run with no failures before any major commits.
* "spec" -- tests of features that only involve end user use of the Java EE 7 spec APIs. Should be run with no failures before any major commits.

For basic smoke tests, simply: `mvn test`

To run all the tests

    mvn install -DallTests

Using Eclipse
-------------
1. Install the latest version of eclipse
2. Make sure Xmx in eclipse.ini is at least 1280M, and it's using Java 8
3. Launch eclipse and install the m2e plugin, make sure it uses your repo configs
   (get it from: http://www.eclipse.org/m2e/
   or install "Maven Integration for Eclipse" from the Eclipse Marketplace)
4. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted
   set forbidden reference to WARNING
5. In eclipse preferences Java->Code Style, import the cleanup, templates, and
   formatter configs in [ide-configs/eclipse](https://github.com/wildfly/wildfly-core/tree/master/ide-configs) in the wildfly-core repository.
6. In eclipse preferences Java->Editor->Save Actions enable "Additional Actions",
   and deselect all actions except for "Remove trailing whitespace"
7. Use import on the root pom, which will pull in all modules
8. Wait (m2e takes a while on initial import)

License
-------
* [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)

