@echo off
rem -------------------------------------------------------------------------
rem JBoss Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem Use --debug to activate debug mode with an optional argument to specify the port
rem Usage : standalone.bat --debug
rem         standalone.bat --debug 9797

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

rem By default debug mode is disable.
set DEBUG_MODE=false
set DEBUG_PORT=8787
rem Set to all parameters by default
set "SERVER_OPTS=%*"

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=standalone.bat"
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

rem Read command-line args.
:READ-ARGS
if "%1" == "" (
   goto MAIN
) else if "%1" == "--debug" (
   goto READ-DEBUG-PORT
) else (
   rem This doesn't work as Windows splits on = and spaces by default
   rem set SERVER_OPTS=%SERVER_OPTS% %1
   shift
   goto READ-ARGS
)

:READ-DEBUG-PORT
set "DEBUG_MODE=true"
set DEBUG_ARG="%2"
if not "x%DEBUG_ARG" == "x" (
   if x%DEBUG_ARG:-=%==x%DEBUG_ARG% (
      shift
      set DEBUG_PORT=%DEBUG_ARG%
   )
   shift
   goto READ-ARGS
)

:MAIN
rem $Id$
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


rem Set debug settings if not already set
if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set "JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=n"
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

set DIRNAME=

rem Setup JBoss specific properties

rem Setup directories, note directories with spaces do not work
set "CONSOLIDATED_OPTS=%JAVA_OPTS% %SERVER_OPTS%"
:DIRLOOP
echo(%CONSOLIDATED_OPTS% | findstr /r /c:"^-Djboss.server.base.dir" > nul && (
  for /f "tokens=1,2* delims==" %%a IN ("%CONSOLIDATED_OPTS%") DO (
    for /f %%i IN ("%%b") DO set "JBOSS_BASE_DIR=%%~fi"
  )
)
echo(%CONSOLIDATED_OPTS% | findstr /r /c:"^-Djboss.server.config.dir" > nul && (
  for /f "tokens=1,2* delims==" %%a IN ("%CONSOLIDATED_OPTS%") DO (
    for /f %%i IN ("%%b") DO set "JBOSS_CONFIG_DIR=%%~fi"
  )
)
echo(%CONSOLIDATED_OPTS% | findstr /r /c:"^-Djboss.server.log.dir" > nul && (
  for /f "tokens=1,2* delims==" %%a IN ("%CONSOLIDATED_OPTS%") DO (
    for /f %%i IN ("%%b") DO set "JBOSS_LOG_DIR=%%~fi"
  )
)

for /f "tokens=1* delims= " %%i IN ("%CONSOLIDATED_OPTS%") DO (
  if %%i == "" (
    goto ENDDIRLOOP
  ) else (
    set CONSOLIDATED_OPTS=%%j
    GOTO DIRLOOP
  )
)

:ENDDIRLOOP

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
  set  "JBOSS_CONFIG_DIR=%JBOSS_BASE_DIR%\configuration"
)

rem Setup JBoss specific properties
set "JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%"

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  if not exist "%JAVA_HOME%" (
    echo JAVA_HOME "%JAVA_HOME%" path doesn't exist
    goto END
  ) else (
    echo Setting JAVA property to "%JAVA_HOME%\bin\java"
    set "JAVA=%JAVA_HOME%\bin\java"
  )
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
)

rem EAP6-121 feature disabled
rem if not "%PRESERVE_JAVA_OPTS%" == "true" (
  rem Add rotating GC logs, if supported, and not already defined
  rem echo "%JAVA_OPTS%" | findstr /I "\-verbose:gc" > nul
  rem if errorlevel == 1 (
    rem Back up any prior logs
    rem move /y "%JBOSS_LOG_DIR%\gc.log.0" "%JBOSS_LOG_DIR%\backupgc.log.0" > nul 2>&1
    rem move /y "%JBOSS_LOG_DIR%\gc.log.1" "%JBOSS_LOG_DIR%\backupgc.log.1" > nul 2>&1
    rem move /y "%JBOSS_LOG_DIR%\gc.log.2" "%JBOSS_LOG_DIR%\backupgc.log.2" > nul 2>&1
    rem move /y "%JBOSS_LOG_DIR%\gc.log.3" "%JBOSS_LOG_DIR%\backupgc.log.3" > nul 2>&1
    rem move /y "%JBOSS_LOG_DIR%\gc.log.4" "%JBOSS_LOG_DIR%\backupgc.log.4" > nul 2>&1
    rem move /y "%JBOSS_LOG_DIR%\gc.log.*.current" "%JBOSS_LOG_DIR%\backupgc.log.current" > nul 2>&1
    rem "%JAVA%" -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -Xloggc:%XLOGGC% -XX:-TraceClassUnloading -version > nul 2>&1
    rem if not errorlevel == 1 (
      rem if not exist "%JBOSS_LOG_DIR" > nul 2>&1 (
        rem mkdir "%JBOSS_LOG_DIR%"
      rem )
     rem set XLOGGC="%JBOSS_LOG_DIR%\gc.log"
     rem set "JAVA_OPTS=-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -XX:-TraceClassUnloading %JAVA_OPTS%"
    rem )
  rem )
rem )

rem Find jboss-modules.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)



echo ===============================================================================
echo.
echo   JBoss Bootstrap Environment
echo.
echo   JBOSS_HOME: "%JBOSS_HOME%"
echo.
echo   JAVA: "%JAVA%"
echo.
echo   JAVA_OPTS: "%JAVA_OPTS%"
echo.
echo ===============================================================================
echo.

:RESTART
rem if x%XLOGGC% == x (
  "%JAVA%" %JAVA_OPTS% ^
   "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\server.log" ^
   "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
      -jar "%JBOSS_HOME%\jboss-modules.jar" ^
      -mp "%JBOSS_MODULEPATH%" ^
      -jaxpmodule "javax.xml.jaxp-provider" ^
       org.jboss.as.standalone ^
      "-Djboss.home.dir=%JBOSS_HOME%" ^
       %SERVER_OPTS%
rem ) else (
  rem "%JAVA%" -Xloggc:%XLOGGC% %JAVA_OPTS% ^
   rem "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\server.log" ^
   rem "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
      rem -jar "%JBOSS_HOME%\jboss-modules.jar" ^
      rem -mp "%JBOSS_MODULEPATH%" ^
      rem -jaxpmodule "javax.xml.jaxp-provider" ^
      rem org.jboss.as.standalone ^
      rem "-Djboss.home.dir=%JBOSS_HOME%" ^
      rem %SERVER_OPTS%
rem )

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
