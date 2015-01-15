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

Ensure you have JDK 7 (or newer) installed

> java -version

On *nix-like system use the prepared script

> ./build.sh

On Windows use the corresponding batch script

> build.bat

If you already have Maven 3.2.1 (or newer) installed you can use it directly

> mvn install


Starting and Stopping WildFly 
------------------------------------------
Change to the bin directory after a successful build

> $ cd build/target/wildfly-\[version\]/bin

Start the server in domain mode

> $ ./domain.sh

Start the server in standalone mode

> $ ./standalone.sh

To stop the server, press Ctrl + C, or use the admin console

> $ ./jboss-cli.sh --connect command=:shutdown

More information: https://docs.jboss.org/author/display/WFLY9/Getting+Started+Guide

Contributing
------------------
https://community.jboss.org/wiki/HackingOnWildFly

Running the Testsuite
--------------------
The testsuite module contains several submodules including the following:

* "smoke" -- core tests that should be run as part of every build of the AS. Failures here will fail the build.
* "api" -- tests of features that involve end user use of the public JBoss AS 8 API. Should be run with no failures before any major commits.
* "cluster" -- tests of the WildFly HA clustering features. Should be run with no failures before any major commits.
* "domain" -- tests of the domain management features. Should be run with no failures before any major commits.
* "integration" -- tests of a WildFly standalone server's internals. Should be run with no failures before any major commits.
* "spec" -- tests of features that only involve end user use of the Java EE 7 spec APIs. Should be run with no failures before any major commits.

To run the basic testsuite including smoke tests from the root directory, run the build script "./build.sh" or "build.bat":

For basic smoke tests, simply: "./build.sh test"

To run all the tests

> $ ./build.sh install -DallTests

Using Eclipse
-------------
1. Install the latest version of eclipse
2. Make sure Xmx in eclipse.ini is at least 1280M, and it's using java 7
3. Launch eclipse and install the m2e plugin, make sure it uses your repo configs
   (get it from: http://www.eclipse.org/m2e/download/
   or install "Maven Integration for Eclipse" from the Eclipse Marketplace)
4. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted
   set forbidden reference to WARNING
5. In eclipse preferences Java->Code Style, import the cleanup, templates, and
   formatter configs in [ide-configs/eclipse](https://github.com/wildfly/wildfly-core/tree/master/ide-configs) in the wildfly-core repository.
6. In eclipse preferences Java->Editor->Save Actions enable "Additional Actions",
   and deselect all actions except for "Remove trailing whitespace"
7. Use import on the root pom, which will pull in all modules
8. Wait (m2e takes awhile on initial import)

License
-------
* [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)

