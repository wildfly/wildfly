@echo off
rem -------------------------------------------------------------------------
rem Add User script for Windows
rem -------------------------------------------------------------------------
rem
rem A simple utility for adding new users to the properties file used 
rem for domain management authentication out of the box.

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd %DIRNAME%..
set "RESOLVED_WILDFLY_HOME=%CD%"
popd

if "x%WILDFLY_HOME%" == "x" (
  set "WILDFLY_HOME=%RESOLVED_WILDFLY_HOME%"
)

pushd "%WILDFLY_HOME%"
set "SANITIZED_WILDFLY_HOME=%CD%"
popd

if "%RESOLVED_WILDFLY_HOME%" NEQ "%SANITIZED_WILDFLY_HOME%" (
    echo WARNING WILDFLY_HOME may be pointing to a different installation - unpredictable results may occur.
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
if exist "%WILDFLY_HOME%\jboss-modules.jar" (
    set "RUNJAR=%WILDFLY_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%WILDFLY_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Setup JBoss specific properties

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%WILDFLY_HOME%\modules"
)

rem Uncomment to override standalone and domain user location  
rem set "JAVA_OPTS=%JAVA_OPTS% -Djboss.server.config.user.dir=..\standalone\configuration -Djboss.domain.config.user.dir=..\domain\configuration"

"%JAVA%" %JAVA_OPTS% ^
    -jar "%WILDFLY_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_MODULEPATH%" ^
     org.jboss.as.domain-add-user ^
     %*

:END
if "x%NOPAUSE%" == "x" pause
