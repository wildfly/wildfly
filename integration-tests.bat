@echo off
REM  ======================================================================
REM
REM  A script to run the integration tests on Windows
REM
REM  ======================================================================
REM
REM Authors:
REM     Jason Dillon <jason@planet57.com>
REM     Sacha Labourey <sacha.labourey@cogito-info.ch>
REM

REM ******************************************************
REM Ignore the users classpath, cause it might mess
REM things up
REM ******************************************************

SETLOCAL

set PROGNAME=%0
set DIRNAME=%~p0

set CLASSPATH=

REM MAVEN_OPTS MAVEN_OPTS now live in .mvn/jvm.config and .mvn/maven.config
REM set MAVEN_OPTS=%MAVEN_OPTS% -Xmx768M


REM  Support for testsuite profile processing
set CMD_LINE_PARAMS=
set TESTS_SPECIFIED=N

REM  Each test module executes a different type of test
set INTEGRATION_TESTS=-Dintegration.module -Dbasic.integration.tests -Dcompat.integration.tests -Dclustering.integration.tests -Dtimerservice.integration.tests
set SMOKE_TESTS=-Dintegration.module -Dsmoke.integration.tests
set DOMAIN_TESTS=-Ddomain.module
set COMPAT_TESTS=-Dcompat.module


set MVN=%DIRNAME%\mvn.cmd
set GOAL=%2
if "%GOAL%"=="" set GOAL=install

REM WFLY-8175 requires that we keep installing Maven under the tools directory
REM the current project, at least when mvnw is invoked from build and integration-tests
REM scripts
set GOAL=-Dmaven.user.home=%DIRNAME%\tools %GOAL%

REM  Process test directives before calling maven
call :processTestDirectives %GOAL% %3 %4 %5 %6 %7 %8

REM  Change to testsuite directory before executing mvn.
cd %DIRNAME%\testsuite

echo Calling "%MVN%" %CMD_LINE_PARAMS%
call "%MVN%" %CMD_LINE_PARAMS%

cd %DIRNAME%

REM  Pause the batch script when maven terminates.
if "%NOPAUSE%" == "" pause

goto :EOF

REM ******************************************************
REM ***  Function to process testsuite directives.     ***
REM ******************************************************
:processTestDirectives

REM echo "Calling processTestDirectives %*"
:loop

REM  Check if we have no more parameters to process.
if "%1" == "" (
  if "%TESTS_SPECIFIED%" == "N" set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %SMOKE_TESTS%"
  goto :eof
)
REM  Replace occurrences of directives with corresponding maven profiles
REM  -DallTests
if "%1" == "-DallTests" (
  set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %INTEGRATION_TESTS% %DOMAIN_TESTS% %COMPAT_TESTS% %SMOKE_TESTS%"
  set "TESTS_SPECIFIED=Y"
  goto processed
)
REM  -Ddomain-tests
if "%1" == "-Ddomain-tests" (
  set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %DOMAIN_TESTS%"
  set "TESTS_SPECIFIED=Y"
  goto processed
)
REM  -Dcompat-tests
if "%1" == "-Dcompat-tests" (
  set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %COMPAT_TESTS%"
  set "TESTS_SPECIFIED=Y"
  goto processed
)
REM  -Dsmoke-tests
if "%1" == "-Dsmoke-tests" (
  set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %SMOKE_TESTS%"
  set "TESTS_SPECIFIED=Y"
  goto processed
)
REM  Pass through other params.
set "CMD_LINE_PARAMS=%CMD_LINE_PARAMS% %1"

:processed
shift
goto loop
