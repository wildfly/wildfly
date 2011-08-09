@echo off
rem -------------------------------------------------------------------------
rem JBoss Admin CLI Script for Windows
rem -------------------------------------------------------------------------

rem $Id$

if not "x%ECHO%" == "x"  echo %ECHO%

if "%OS%" == "Windows_NT" (
  setlocal
  set DIRNAME=%~dp0%
) else (
  set DIRNAME=.\
)

rem Change into bin\ directory and set JBOSS_HOME as parent
pushd %DIRNAME%..
if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%CD%"
)
popd

set DIRNAME=

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=jboss-admin.bat"
)

rem Setup JBoss specific properties
set JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set JAVA=%JAVA_HOME%\bin\java
)

rem Find jboss-modules.jar, or we can't continue
set RUNJAR=%JBOSS_HOME%\jboss-modules.jar
if not exist "%RUNJAR%" (
  echo Could not locate "%RUNJAR%".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

"%JAVA%" %JAVA_OPTS% -jar "%RUNJAR%" ^
    -logmodule org.jboss.logmanager  ^
    -mp "%JBOSS_HOME%\modules" ^
     org.jboss.as.cli ^
     %*

:END
if "x%NOPAUSE%" == "x" pause
