@echo off
REM  ======================================================================
REM
REM  A build script
REM
REM  At present this script does nothing else than pass the arguments by to
REM  mvnw and you are encouraged to use mvnw (a.k.a. Maven Wrapper)
REM  directly.
REM
REM  Note that in the past, this script took the following responsibilities
REM  that are now handled by mvnw or Maven itself:
REM
REM  * Download and install a specific version of Maven
REM  * Set Maven options via MAVEN_OPTS environment variable - these can
REM    now be set in .mvn/jvm.config and .mvn/maven.config
REM
REM  ======================================================================
REM
REM Authors:
REM     Jason Dillon <jason@planet57.com>
REM     Sacha Labourey <sacha.labourey@cogito-info.ch>
REM

REM ******************************************************
REM Ignore the MAVEN_HOME variable: we want to use *our*
REM Maven version and associated JARs.
REM ******************************************************
REM Ignore the users classpath, cause it might mess
REM things up
REM ******************************************************

SETLOCAL

set CLASSPATH=
set M2_HOME=
set MAVEN_HOME=

REM MAVEN_OPTS MAVEN_OPTS now live in .mvn/jvm.config and .mvn/maven.config
REM set MAVEN_OPTS=%MAVEN_OPTS% -Xmx768M

set DIRNAME=%~p0
set MVN=%DIRNAME%\mvnw.cmd

set GOAL=%1
if "%GOAL%"=="" set GOAL=install

REM WFLY-8175 requires that we keep installing Maven under the tools directory
REM the current project, at least when mvnw is invoked from build and integration-tests
REM scripts
set GOAL=-Dmaven.user.home=%DIRNAME%\tools %GOAL%

echo Calling "%MVN%" %GOAL% %2 %3 %4 %5 %6 %7
call "%MVN%" %GOAL% %2 %3 %4 %5 %6 %7

if "%NOPAUSE%" == "" pause
