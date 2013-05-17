@echo off
rem -------------------------------------------------------------------------
rem JBoss Application Client Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

rem Read an optional configuration file.
if "x%APPCLIENT_CONF%" == "x" (
   set "APPCLIENT_CONF=%DIRNAME%appclient.conf.bat"
)
if exist "%APPCLIENT_CONF%" (
   echo Calling %APPCLIENT_CONF%
   call "%APPCLIENT_CONF%" %*
) else (
   echo Config file not found %APPCLIENT_CONF%
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
  set "PROGNAME=appclient.bat"
)

rem Setup JBoss specific properties
set JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem Add -server to the JVM options, if supported
"%JAVA%" -server -version 2>&1 | findstr /I hotspot > nul
if not errorlevel == 1 (
  set "JAVA_OPTS=%JAVA_OPTS% -server"
)

rem Find run.jar, or we can't continue
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

"%JAVA%" %JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%UNQUOTED_JBOSS_HOME%\appclient\log\appclient.log" ^
 "-Dlogging.configuration=file:%UNQUOTED_JBOSS_HOME%/appclient/configuration/logging.properties" ^
    -jar "%UNQUOTED_JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_MODULEPATH%" ^
    -jaxpmodule "javax.xml.jaxp-provider" ^
     org.jboss.as.appclient ^
    -Djboss.home.dir="%UNQUOTED_JBOSS_HOME%" ^
    -Djboss.server.base.dir="%UNQUOTED_JBOSS_HOME%\appclient" ^
     %*
