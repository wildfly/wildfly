@echo off
rem -------------------------------------------------------------------------
rem JBoss Domain Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem $Id: $

if not "%ECHO%" == ""  echo %ECHO%

if "%OS%" == "Windows_NT" (
  setlocal
  set DIRNAME="%~dp0%"
  set PROGNAME="%~nx0%"
) else (
  set DIRNAME=.\
  set PROGNAME=domain.bat
)

rem If JBOSS_HOME is not specified, use parent of script
rem Will be needed in domain.conf.bat
if "x%JBOSS_HOME%" == "x" (
  pushd %DIRNAME%..
  set "JBOSS_HOME=%CD%"
  popd
)

rem Read an optional configuration file.
if "x%DOMAIN_CONF%" == "x" (   
   set DOMAIN_CONF="%DIRNAME%domain.conf.bat"
)
if exist "%DOMAIN_CONF%" (
   echo Calling "%DOMAIN_CONF%".
   call "%DOMAIN_CONF%" %*
) else (
   echo Config file not found "%DOMAIN_CONF%".
)

set DIRNAME=


rem Setup JBoss specific properties
set JAVA_OPTS=-Dprogram.name="%PROGNAME%" %JAVA_OPTS%

if "x%JAVA_HOME%" == "x" (
  set JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set JAVA="%JAVA_HOME%\bin\java"
)

rem Add -server to the JVM options, if supported
"%JAVA%" -server -version 2>&1 | findstr /I hotspot > nul
if not errorlevel == 1 (
  set JAVA_OPTS=%JAVA_OPTS% -server
)

rem Find jboss-modules.jar, or we can't continue
set RUNJAR="%JBOSS_HOME%\jboss-modules.jar"
if not exist "%RUNJAR%" (
  echo Could not locate "%RUNJAR%".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Setup JBoss specific properties

rem Setup the java endorsed dirs
set JBOSS_ENDORSED_DIRS="%JBOSS_HOME%\lib\endorsed"

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
"%JAVA%" %JAVA_OPTS% ^
 -Dorg.jboss.boot.log.file="%JBOSS_HOME%\process-controller\log\boot.log" ^
 -Dlogging.configuration="file:%JBOSS_HOME%/domain/configuration/logging.properties" ^
    -jar "%RUNJAR%" ^
    -mp "%JBOSS_HOME%\modules" ^
    -logmodule org.jboss.logmanager ^
     org.jboss.as.process-controller ^
    -jboss-home "%JBOSS_HOME%" ^
    -jvm "%JAVA%" ^
    -- ^
    -Dorg.jboss.boot.log.file="%JBOSS_HOME%\domain\log\host-controller\boot.log" ^
    -Dlogging.configuration="file:%JBOSS_HOME%/domain/configuration/logging.properties" ^
    %JAVA_OPTS% ^
    -- ^
    -default-jvm "%JAVA%" ^
    %*

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
