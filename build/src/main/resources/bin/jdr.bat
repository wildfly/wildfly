@echo off
rem -------------------------------------------------------------------------
rem JBoss Diagnostic Report (JDR) Script for Windows
rem -------------------------------------------------------------------------
rem
rem This script creates a JDR report containing useful information for
rem diagnosing problems with the application server.  The report consists 
rem of a zip file containing log files, configuration, a list of all files
rem in the distribution and, if available, runtime metrics.

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd %DIRNAME%..
set "RESOLVED_JBOSS_HOME=%CD%"
popd

set UNQUOTED_JBOSS_HOME=%JBOSS_HOME:"=%
rem attempt to unquote again to remove quote if envvar was not set
set UNQUOTED_JBOSS_HOME=%UNQUOTED_JBOSS_HOME:"=%
set QUOTED_JBOSS_HOME="%UNQUOTED_JBOSS_HOME%"
rem should only a = if envvar was not set
if "%UNQUOTED_JBOSS_HOME%" == "=" (
  set "UNQUOTED_JBOSS_HOME=%RESOLVED_JBOSS_HOME%"
  set QUOTED_JBOSS_HOME="%RESOLVED_JBOSS_HOME%"
  set "JBOSS_HOME=%%RESOLVED_JBOSS_HOME%"
)
pushd %QUOTED_JBOSS_HOME%
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if /i "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
   echo.
   echo   WARNING:  JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
   echo.
   echo       JBOSS_HOME: %QUOTED_JBOSS_HOME%
   echo.
)

set DIRNAME=

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=jdr.bat"
)

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem Find jboss-modules.jar, or we can't continue
if exist "%UNQUOTED_JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%UNQUOTED_JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%UNQUOTED_JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Setup JBoss specific properties

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%UNQUOTED_JBOSS_HOME%\modules"
)

"%JAVA%" ^
    -Djboss.home.dir="%UNQUOTED_JBOSS_HOME%" ^
    -jar "%UNQUOTED_JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_MODULEPATH%" ^
     org.jboss.as.jdr ^
     %*
