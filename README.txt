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
To use eclipse you need to use the m2eclipse plugin (http://m2eclipse.sonatype.org/).
The following steps are recommended:
1. Install the latest version of eclipse
2. Set Xmx in eclipse.ini to be at least 512M, and make sure it's using java 6
3. On the command line run ./build.sh eclipse:m2eclipse
4. launch eclipse and install the m2eclipse plugin, and make sure it uses your repo configs
5. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted set forbidden reference to WARNING
6. In eclipse preferences Java->Code Style, import the cleanup, templates, and formatter configs in ide-configs/eclipse
7. Use import on the root pom, which will pull in all modules
8. Wait (m2eclipse takes awhile on initial import, especially if you did not do step 3)
