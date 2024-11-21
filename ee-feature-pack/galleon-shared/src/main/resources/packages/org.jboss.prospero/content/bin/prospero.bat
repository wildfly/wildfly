@echo off
rem -------------------------------------------------------------------------
rem JBoss Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem Use --debug to activate debug mode with an optional argument to specify the port
rem Usage : ${prospero.dist.name}.bat --debug
rem         ${prospero.dist.name}.bat --debug 9797

@if not "%ECHO%" == ""  echo %ECHO%
setlocal

rem By default debug mode is disable.
set DEBUG_MODE=false
set DEBUG_PORT_VAR=8787
rem Set to all parameters by default
set "SERVER_OPTS=%*"

if NOT "x%DEBUG%" == "x" (
  set "DEBUG_MODE=%DEBUG%"
)

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=${prospero.dist.name}.bat"
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

rem Read command-line args, the ~ removes the quotes from the parameter
set LOAD_COMMON=true
set PARAMS=
:READ-ARGS
if "%~1" == "" (
   goto MAIN
) else if "%~1" == "--debug" (
   goto READ-DEBUG-PORT
) else if "%~1" == "-secmgr" (
   set SECMGR=true
) else if "%~1" == "--no-load-common" (
   set LOAD_COMMON=false
) else (
   set "PARAMS=%PARAMS%%1 "
)
shift
goto READ-ARGS

:READ-DEBUG-PORT
set "DEBUG_MODE=true"
set DEBUG_ARG="%2"
if not %DEBUG_ARG% == "" (
   if x%DEBUG_ARG:-=%==x%DEBUG_ARG% (
      shift
      set DEBUG_PORT_VAR=%DEBUG_ARG%
   )
   shift
   goto READ-ARGS
)

:MAIN

setlocal EnableDelayedExpansion
if "%LOAD_COMMON%" == "true" (
   call "!DIRNAME!common.bat" :commonConf
)
rem check for the security manager system property
echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager" > nul
if not errorlevel == 1 (
    echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager=allow" > nul
    if errorlevel == 1 (
        echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
        GOTO :EOF
    )
)
setlocal DisableDelayedExpansion


rem $Id$

pushd "%DIRNAME%.."
set "RESOLVED_PROSPERO_HOME=%CD%"
popd

if "x%PROSPERO_HOME%" == "x" (
  set "PROSPERO_HOME=%RESOLVED_PROSPERO_HOME%"
)

pushd "%PROSPERO_HOME%"
set "SANITIZED_PROSPERO_HOME=%CD%"
popd

if /i "%RESOLVED_PROSPERO_HOME%" NEQ "%SANITIZED_PROSPERO_HOME%" (
   echo.
   echo   WARNING:  PROSPERO_HOME may be pointing to a different installation - unpredictable results may occur.
   echo.
   echo       PROSPERO_HOME: "%PROSPERO_HOME%"
   echo.
)

rem Set debug settings if not already set
if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set "JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT_VAR%,server=y,suspend=n"
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

rem Set default logging configuration
echo "%JAVA_OPTS%" | findstr /I "logging.configuration" > nul
if errorlevel == 1 (
  set "JAVA_OPTS=%JAVA_OPTS% -Dlogging.configuration="file:%PROSPERO_HOME%\bin\${prospero.dist.name}-logging.properties""
)

rem Set default log location
echo "%JAVA_OPTS%" | findstr /I "org.wildfly.prospero.log.file" > nul
if errorlevel == 1 (
    set "JAVA_OPTS=%JAVA_OPTS% -Dorg.wildfly.prospero.log.file="%PROSPERO_HOME%\@prospero.log.location.win@""
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
     if not exist "%JAVA_HOME%\bin\java.exe" (
       echo "%JAVA_HOME%\bin\java.exe" does not exist
       goto END_NO_PAUSE
     )
    set "JAVA=%JAVA_HOME%\bin\java"
  )
)

"%JAVA%" --add-modules=java.se -version >nul 2>&1 && (set MODULAR_JDK=true) || (set MODULAR_JDK=false)

setlocal EnableDelayedExpansion

if not "%PRESERVE_JAVA_OPTS%" == "true" (
    rem Add -client to the JVM options, if supported (32 bit VM), and not overriden
    echo "%JAVA_OPTS%" | findstr /I "\-client \-server" > nul
    if errorlevel == 1 (
        set JVM_OPTVERSION=-version
        echo "%JAVA_OPTS%" | findstr /I \-d64 > nul
        if not errorlevel == 1 (
            set "JVM_OPTVERSION=-d64 !JVM_OPTVERSION!"
            goto :CHECK_VERSION
        )
        echo "%JAVA_OPTS%" | findstr /I \-d32 > nul
        if not errorlevel == 1 (
            set "JVM_OPTVERSION=-d32 !JVM_OPTVERSION!"
            goto :CHECK_VERSION
        )
        goto :CHECK_SUPPORTED

        :CHECK_VERSION
        "%JAVA%" !JVM_OPTVERSION! 2>&1 | findstr /I /C:"Unrecognized option" > nul
        if not errorlevel == 1 (
            set JVM_OPTVERSION=-version
        )

        :CHECK_SUPPORTED
        "%JAVA%" !JVM_OPTVERSION! 2>&1 | findstr /I /C:"Client VM" > nul
        if not errorlevel == 1 (
            set "JAVA_OPTS=-client %JAVA_OPTS%"
            goto :SET_SERVER_END
        )
        "%JAVA%" !JVM_OPTVERSION! 2>&1 | findstr /I /C:"hotspot" /C:"openJDK" /C:"IBM J9" > nul
        if not errorlevel == 1 (
            set "JAVA_OPTS=-server %JAVA_OPTS%"
        )
    )
)
:SET_SERVER_END

setlocal DisableDelayedExpansion

rem Find jboss-modules.jar, or we can't continue
if exist "%PROSPERO_HOME%\jboss-modules.jar" (
    set "RUNJAR=%PROSPERO_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%PROSPERO_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Use a copy of jboss-modules to avoid locking issues when jboss-modules is updated
:COPY_JBOSS_MODULES
    set "TMP_JBOSS_MODULES=%PROSPERO_HOME%\jboss-modules~%RANDOM%.tmp"
    rem loop while we find a non-existing filename
    if exist "%TMP_JBOSS_MODULES%" goto :COPY_JBOSS_MODULES
    copy "%RUNJAR%" "%TMP_JBOSS_MODULES%"

rem If the -Djava.security.manager is found, enable the -secmgr and include a bogus security manager for JBoss Modules to replace
echo(!JAVA_OPTS! | findstr /r /c:"-Djava.security.manager" > nul && (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%PROSPERO_HOME%\modules"
)

setlocal EnableDelayedExpansion
call "!DIRNAME!common.bat" :setModularJdk
setlocal DisableDelayedExpansion

if not "%PRESERVE_JAVA_OPT%" == "true" (

    rem set default modular jvm parameters
    setlocal EnableDelayedExpansion
    call "!DIRNAME!common.bat" :setDefaultModularJvmOptions !JAVA_OPTS!
    set "JAVA_OPTS=!JAVA_OPTS! !DEFAULT_MODULAR_JVM_OPTIONS!"

    rem Set default Security Manager configuration value
    call "!DIRNAME!common.bat" :setSecurityManagerDefault
    set "JAVA_OPTS=!JAVA_OPTS! !SECURITY_MANAGER_CONFIG_OPTION!"
    setlocal DisableDelayedExpansion
)




rem Set the module options
set "MODULE_OPTS=%MODULE_OPTS%"
if "%SECMGR%" == "true" (
    set "MODULE_OPTS=%MODULE_OPTS% -secmgr"
)
setlocal EnableDelayedExpansion
rem Add -client to the JVM options, if supported (32 bit VM), and not overridden
echo "!MODULE_OPTS!" | findstr /I \-javaagent: > nul
if not errorlevel == 1 (
    set AGENT_PARAM=-javaagent:"!TMP_JBOSS_MODULES!"
    set "JAVA_OPTS=!AGENT_PARAM! !JAVA_OPTS!"
)
setlocal DisableDelayedExpansion

:RESTART
  "%JAVA%" %JAVA_OPTS% ^
      -jar "%TMP_JBOSS_MODULES%" ^
      %MODULE_OPTS% ^
      -mp "%JBOSS_MODULEPATH%" ^
      org.jboss.prospero ^
      %PARAMS%

if %errorlevel% equ 10 (
    echo Restarting...
    goto RESTART
) else (
    goto END_NO_PAUSE
)

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
set EXIT_LEVEL="%errorlevel%"
if exist "%TMP_JBOSS_MODULES%" del /Q "%TMP_JBOSS_MODULES%"
exit /b %EXIT_LEVEL%
