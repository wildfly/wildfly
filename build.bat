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
REM ************ Test if Maven Batch file exists ***********
REM ******************************************************

:testIfExists
if exist %1 call :BatchFound %1 %2 %3 %4 %5 %6 %7 %8

goto :EOF


REM ******************************************************
REM ************** Batch file has been found *************
REM ******************************************************

:BatchFound
if (%EXECUTED%)==(FALSE) call :ExecuteBatch %1 %2 %3 %4 %5 %6 %7 %8
set EXECUTED=TRUE

goto :EOF

REM ******************************************************
REM ************* Execute Batch file only once ***********
REM ******************************************************

:ExecuteBatch
echo Calling %1 %2 %3 %4 %5 %6 %7 %8
set GOAL=%2
if "%GOAL%"=="" set GOAL=install

REM run smoke tests by default
set SMOKE_TESTS=-Dintegration.module -Dsmoke.integration.tests

call %1 %GOAL% %SMOKE_TESTS% %3 %4 %5 %6 %7 %8

:end

if "%NOPAUSE%" == "" pause

