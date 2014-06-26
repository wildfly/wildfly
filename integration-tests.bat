@echo off
REM  ======================================================================
REM
REM  This is the main entry point for the build system.
REM
REM  Users should be sure to execute this file rather than 'mvn' to ensure
REM  the correct version is being used with the correct configuration.
REM
REM  ======================================================================
REM
REM $Id: build.bat 105735 2010-06-04 19:45:13Z pgier $
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

set PROGNAME=%0
set DIRNAME=%~p0

set CLASSPATH=
set M2_HOME=
set MAVEN_HOME=
set MAVEN_OPTS=%MAVEN_OPTS% -Xmx768M

powershell "& "tools\download-maven.ps1"

REM ******************************************************
REM - "for" loops have been unrolled for compatibility
REM   with some WIN32 systems.
REM ******************************************************

set NAMES=tools;tools\maven;tools\apache\maven
set SUBFOLDERS=.;..;..\..;..\..\..;..\..\..\..

REM ******************************************************
REM ******************************************************

SET EXECUTED=FALSE
for %%i in (%NAMES%) do call :subLoop %%i %1 %2 %3 %4 %5 %6

goto :EOF


REM ******************************************************
REM ********* Search for names in the subfolders *********
REM ******************************************************

:subLoop
for %%j in (%SUBFOLDERS%) do call :testIfExists %%j\%1\bin\mvn.bat %2 %3 %4 %5 %6 %7

goto :EOF


REM ******************************************************
REM ***  Test if Maven batch file exists.             ***
REM ******************************************************

:testIfExists
if exist %1 call :BatchFound %1 %2 %3 %4 %5 %6 %7 %8

goto :EOF


REM ******************************************************
REM ***  Batch file has been found.                    ***
REM ******************************************************

:BatchFound
if (%EXECUTED%)==(FALSE) call :ExecuteBatch %1 %2 %3 %4 %5 %6 %7 %8
set EXECUTED=TRUE

goto :EOF

REM ******************************************************
REM ***  Execute batch file only once.                 ***
REM ******************************************************

:ExecuteBatch

REM  Support for testsuite profile processing
set CMD_LINE_PARAMS=
set TESTS_SPECIFIED=N

REM  Each test module executes a different type of test
set INTEGRATION_TESTS=-Dintegration.module -Dbasic.integration.tests -Dcompat.integration.tests -Dclustering.integration.tests -Dtimerservice.integration.tests
set SMOKE_TESTS=-Dintegration.module -Dsmoke.integration.tests
set DOMAIN_TESTS=-Ddomain.module
set COMPAT_TESTS=-Dcompat.module


set MVN=%1%
set GOAL=%2
if "%GOAL%"=="" set GOAL=install

REM  Process test directives before calling maven
call :processTestDirectives %GOAL% %3 %4 %5 %6 %7 %8

REM  Change to testsuite directory before executing mvn.
cd %DIRNAME%\testsuite

echo Calling ..\%MVN% %CMD_LINE_PARAMS%
call ..\%MVN% %CMD_LINE_PARAMS%

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
