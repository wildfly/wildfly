@echo off
rem -------------------------------------------------------------------------
rem JBoss Application Client Bootstrap Script for Windows
rem -------------------------------------------------------------------------

@if not "%ECHO%" == ""  echo %ECHO%

if "%OS%" == "Windows_NT" (
  setlocal
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

if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%"
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
    echo WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
)

set DIRNAME=

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

rem Find jboss-modules.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
    echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
    echo Please check that you are in the bin directory when running this script.
    goto END
)

rem Setup JBoss specific properties

rem Setup the java endorsed dirs
set JBOSS_ENDORSED_DIRS=%JBOSS_HOME%\lib\endorsed

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)


"%JAVA%" %JAVA_OPTS% ^
  "-Dorg.jboss.boot.log.file=%JBOSS_HOME%\appclient\log\boot.log" ^
  "-Dlogging.configuration=file:%JBOSS_HOME%/appclient/configuration/logging.properties" ^
  -jar "%RUNJAR%" ^
    -mp "%JBOSS_MODULEPATH%" ^
    -jaxpmodule "javax.xml.jaxp-provider" ^
    org.jboss.as.appclient ^
    "-Djboss.home.dir=%JBOSS_HOME%" ^
    "-Djboss.server.base.dir=%JBOSS_HOME%\appclient" ^
     %*
