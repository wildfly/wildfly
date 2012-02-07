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
if "x%STANDALONE_CONF%" == "x" (
   set "STANDALONE_CONF=%DIRNAME%standalone.conf.bat"
)
if exist "%STANDALONE_CONF%" (
   echo Calling "%STANDALONE_CONF%"
   call "%STANDALONE_CONF%" %*
) else (
   echo Config file not found "%STANDALONE_CONF%"
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

if not "%PRESERVE_JAVA_OPTS%" == "true" (
  rem Add -client to the JVM options, if supported (32 bit VM), and not overriden
  echo "%JAVA_OPTS%" | findstr /I \-server > nul
  if errorlevel == 1 (
    "%JAVA%" -client -version 2>&1 | findstr /I /C:"Client VM" > nul
    if not errorlevel == 1 (
      set "JAVA_OPTS=-client %JAVA_OPTS%"
    )
  )

  rem Add compressed oops, if supported (64 bit VM), and not overriden
  echo "%JAVA_OPTS%" | findstr /I "\-XX:\-UseCompressedOops \-client" > nul
  if errorlevel == 1 (
    "%JAVA%" -XX:+UseCompressedOops -version > nul 2>&1
    if not errorlevel == 1 (
      set "JAVA_OPTS=-XX:+UseCompressedOops %JAVA_OPTS%"
    )
  )

  rem Add tiered compilation, if supported (64 bit VM), and not overriden
  echo "%JAVA_OPTS%" | findstr /I "\-XX:\-TieredCompilation \-client" > nul
  if errorlevel == 1 (
    "%JAVA%" -XX:+TieredCompilation -version > nul 2>&1
    if not errorlevel == 1 (
      set "JAVA_OPTS=-XX:+TieredCompilation %JAVA_OPTS%"
    )
  )
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

rem Set the standalone base dir
if "x%JBOSS_BASE_DIR%" == "x" (
  set  "JBOSS_BASE_DIR=%JBOSS_HOME%\standalone"
)
rem Set the standalone log dir
if "x%JBOSS_LOG_DIR%" == "x" (
  set  "JBOSS_LOG_DIR=%JBOSS_BASE_DIR%\log"
)
rem Set the standalone configuration dir
if "x%JBOSS_CONFIG_DIR%" == "x" (
  set  "JBOSS_CONFIG_DIR=%JBOSS_BASE_DIR%/configuration"
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
"%JAVA%" %JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\boot.log" ^
 "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    -mp "%JBOSS_MODULEPATH%" ^
    -jaxpmodule "javax.xml.jaxp-provider" ^
     org.jboss.as.standalone ^
    -Djboss.home.dir="%JBOSS_HOME%" ^
     %*

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
