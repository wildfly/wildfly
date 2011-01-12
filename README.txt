JBoss Application Server

Directory Structure
-------------------
build - Contains the build scripts for creating the complete JBoss AS distribution.
testsuite - Contains code and build scripts for testing the application server
tools - Various files used by the build (ant, maven, etc.)

The remaining directories contain the various components of the application server.

Dependencies
------------
The pom.xml in the root of the source checkout contains the Maven configuration which 
controls dependency versions.

Building
-------------------
From the root directory, run the build script "./build.sh" or "build.bat"
If you want to call maven directly "mvn install", you must add the jboss repository 
configuration to your Maven settings.  See the JBoss build wiki for more information about
repository config.

For slightly faster builds, the maven enforcer plugin can be skipped.
./build.sh -P-enforce

Generation of the source jars can be skipping by deactivating the sources
profile.
./build.sh -P-sources

During development you may want to build only a single module and update the 
distribution build.  This can be done using the property "module".
For example, to build the "remoting" module and update the dist build, run the following:
./build.sh -Dmodule=remoting

Running the Testsuite
--------------------
The testsuite module contains four submodules:

1) "smoke" -- core tests that should be run as part of every build of the AS. Failures here will fail the build.
2) "integration" -- the full integration testsuite. Should be run with no failures before any major commits.
3) "benchmark" -- tests used to compare performance against other releases or previous builds
4) "stress" -- tests of the server's ability to perform properly while under stress 

To run the testsuite from the root directory, run the build script "./build.sh" or "build.bat":

For basic smoke tests, simply: "./build.sh test"
For integration tests: "./build.sh test -Pintegration-tests"
For benchmark tests: "./build.sh test -Pbenchmark-tests"
For stress tests: "./build.sh test -Pstress-tests"
For all of the above tests: "./build.sh test -Pall-tests"

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
