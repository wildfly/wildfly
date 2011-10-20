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
if "x%STANDALONE_CONF%" == "x" (   
   set "STANDALONE_CONF=%DIRNAME%standalone.conf.bat"
)
if exist "%STANDALONE_CONF%" (
   echo Calling %STANDALONE_CONF%
   call "%STANDALONE_CONF%" %*
) else (
   echo Config file not found %STANDALONE_CONF%
)

pushd %DIRNAME%..
if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%CD%"
)
popd

set DIRNAME=

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=standalone.bat"
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
if "x%MODULEPATH%" == "x" (
  set  "MODULEPATH=%JBOSS_HOME%\modules"
)


"%JAVA%" %JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%JBOSS_HOME%\standalone\log\boot.log" ^
 "-Dlogging.configuration=file:%JBOSS_HOME%/standalone/configuration/logging.properties" ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%MODULEPATH%" ^
    -logmodule "org.jboss.logmanager" ^
    -jaxpmodule "javax.xml.jaxp-provider" ^
     org.jboss.as.standalone ^
    -Djboss.home.dir="%JBOSS_HOME%" ^
    -Djboss.server.base.dir="%JBOSS_HOME%\appclient" ^
     %*
