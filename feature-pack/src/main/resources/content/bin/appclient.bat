@echo off
rem -------------------------------------------------------------------------
rem JBoss Application Client Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal
rem Set to all parameters by default
set SERVER_OPTS=%*

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
  set "PROGNAME=%~nx0%"
) else (
  set DIRNAME=.\
  set "PROGNAME=appclient.bat"
)

setlocal EnableDelayedExpansion
rem check for the security manager system property
echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager" > nul
if not errorlevel == 1 (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Read command-line args, the ~ removes the quotes from the parameter
:READ-ARGS
if "%~1" == "" (
   goto MAIN
) else if "%~1" == "-secmgr" (
   set SECMGR=true
)
shift
goto READ-ARGS

:MAIN

rem Read an optional configuration file.
if "x%APPCLIENT_CONF%" == "x" (
   set "APPCLIENT_CONF=%DIRNAME%appclient.conf.bat"
)
if exist "%APPCLIENT_CONF%" (
   echo Calling "%APPCLIENT_CONF%"
   call "%APPCLIENT_CONF%" %*
) else (
   echo Config file not found "%APPCLIENT_CONF%"
)

pushd "%DIRNAME%.."
set "RESOLVED_JBOSS_HOME=%CD%"
popd

if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%"
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if /i "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
   echo.
   echo   WARNING:  JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
   echo.
   echo       JBOSS_HOME: "%JBOSS_HOME%"
   echo.
)

set DIRNAME=

rem Setup JBoss specific properties
set "JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%"

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

rem If the -Djava.security.manager is found, enable the -secmgr and include a bogus security manager for JBoss Modules to replace
echo(%JAVA_OPTS% | findstr /r /c:"-Djava.security.manager" > nul && (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)

rem Find run.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Setup JBoss specific properties

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

rem Set the module options
set "MODULE_OPTS="
if "%SECMGR%" == "true" (
    set "MODULE_OPTS=-secmgr"
)

"%JAVA%" %JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%JBOSS_HOME%\appclient\log\appclient.log" ^
 "-Dlogging.configuration=file:%JBOSS_HOME%/appclient/configuration/logging.properties" ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    %MODULE_OPTS% ^
    -mp "%JBOSS_MODULEPATH%" ^
     org.jboss.as.appclient ^
    "-Djboss.home.dir=%JBOSS_HOME%" ^
    "-Djboss.server.base.dir=%JBOSS_HOME%\appclient" ^
     %*
