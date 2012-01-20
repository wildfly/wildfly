@echo off
rem -------------------------------------------------------------------------
rem JBoss Bootstrap Script for Windows
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
if "x%DOMAIN_CONF%" == "x" (
   set "DOMAIN_CONF=%DIRNAME%domain.conf.bat"
)
if exist "%DOMAIN_CONF%" (
   echo Calling "%DOMAIN_CONF%"
   call "%DOMAIN_CONF%" %*
) else (
   echo Config file not found "%DOMAIN_CONF%"
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

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=domain.bat"
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
  set "PROCESS_CONTROLLER_JAVA_OPTS=%PROCESS_CONTROLLER_JAVA_OPTS -server"
  set "HOST_CONTROLLER_JAVA_OPTS=%HOST_CONTROLLER_JAVA_OPTS -server"
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

rem Setup the java endorsed dirs
set JBOSS_ENDORSED_DIRS=%JBOSS_HOME%\lib\endorsed

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

echo ===============================================================================
echo.
echo   JBoss Bootstrap Environment
echo.
echo   JBOSS_HOME: %JBOSS_HOME%
echo.
echo   JAVA: %JAVA%
echo.
echo   JAVA_OPTS: %JAVA_OPTS%
echo.
echo ===============================================================================
echo.

:RESTART
"%JAVA%" %PROCESS_CONTROLLER_JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%JBOSS_HOME%\domain\log\process-controller\boot.log" ^
 "-Dlogging.configuration=file:%JBOSS_HOME%/domain/configuration/logging.properties" ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_MODULEPATH%" ^
     org.jboss.as.process-controller ^
    -jboss-home "%JBOSS_HOME%" ^
    -jvm "%JAVA%" ^
    -- ^
    "-Dorg.jboss.boot.log.file=%JBOSS_HOME%\domain\log\host-controller\boot.log" ^
    "-Dlogging.configuration=file:%JBOSS_HOME%/domain/configuration/logging.properties" ^
    %HOST_CONTROLLER_JAVA_OPTS% ^
    -- ^
    -default-jvm "%JAVA%" ^
    %*

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
