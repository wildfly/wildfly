JBoss Application Server 
========================
http://www.jboss.org/jbossas/

* Fast Startup
* Small Footprint
* Modular Design
* Unified Configuration and Management
* OSGi

And of course Java EE!

Building
-------------------
If you already have Maven 3 installed

> mvn install

If you don't have Maven 3

> ./build.sh

If you really must use Windows and don't have Maven 3

> build.bat

Starting and Stopping JBoss
------------------------------------------
Change to the bin directory after a successful build

> $ cd build/target/jboss-\[version\]/bin

Start the server in domain mode

> $ ./domain.sh

Start the server in standalone mode

> $ ./standalone.sh

To stop the server, press Ctrl + C, or use the admin console

> $ ./jboss-cli.sh --connect command=:shutdown

More information on the wiki: http://community.jboss.org/wiki/JBossAS7UserGuide

Contributing
------------------
http://community.jboss.org/wiki/HackingonAS7

Running the Testsuite
--------------------
The testsuite module contains several submodules including the following:

* "smoke" -- core tests that should be run as part of every build of the AS. Failures here will fail the build.
* "api" -- tests of features that involve end user use of the public JBoss AS 7 API. Should be run with no failures before any major commits.
* "cluster" -- tests of the AS 7 HA clustering features. Should be run with no failures before any major commits.
* "domain" -- tests of the domain management features. Should be run with no failures before any major commits.
* "integration" -- tests of an AS 7 standalone server's internals. Should be run with no failures before any major commits.
* "spec" -- tests of features that only involve end user use of the Java EE 6 spec APIs. Should be run with no failures before any major commits.
* "benchmark" -- tests used to compare performance against other releases or previous builds
* "stress" -- tests of the server's ability to perform properly while under stress 

To run the basic testsuite including smoke tests from the root directory, run the build script "./build.sh" or "build.bat":

For basic smoke tests, simply: "./build.sh test"

For benchmark tests: "./build.sh test -Pbenchmark-tests"

For stress tests: "./build.sh test -Pstress-tests"

To run all the tests

> $ ./build.sh install -PallTests

Using Eclipse
-------------
1. Install the latest version of eclipse
2. Make sure Xmx in eclipse.ini is at least 512M, and it's using java 6
3. Launch eclispe and install the m2eclipse plugin, make sure it uses your repo configs
   (get it from: http://m2eclipse.sonatype.org/sites/m2e)
4. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted
   set forbidden reference to WARNING
5. In eclipse preferences Java->Code Style, import the cleanup, templates, and
   formatter configs in ide-configs/eclipse
6. In eclipse preferences Java->Editor->Save Actions enable "Additional Actions",
   and deselect all actions except for "Remove trailing whitespace"
7. Use import on the root pom, which will pull in all modules
8. Wait (m2eclipse takes awhile on initial import)

License
-------
* [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)

