@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

setlocal EnableDelayedExpansion
call "!DIRNAME!common.bat" :commonConf
rem check for the security manager system property
echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager" > nul
if not errorlevel == 1 (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

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

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem set default modular jvm parameters
setlocal EnableDelayedExpansion
call "!DIRNAME!common.bat" :setDefaultModularJvmOptions "!JAVA_OPTS!"
set "JAVA_OPTS=!JAVA_OPTS! !DEFAULT_MODULAR_JVM_OPTIONS!"
setlocal DisableDelayedExpansion

rem Find jboss-modules.jar, or we can't continue
set "JBOSS_RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
if not exist "%JBOSS_RUNJAR%" (
  echo Could not locate "%JBOSS_RUNJAR%".
  echo Please check that you are in the bin directory when running this script.
  goto END
)


rem Set the module options
set "MODULE_OPTS="
if "%SECMGR%" == "true" (
    set "MODULE_OPTS=-secmgr"
)


rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

set "JAVA_OPTS=%JAVA_OPTS% -Dprogram.name=wsprovide.bat"

"%JAVA%" %JAVA_OPTS% ^
    -jar "%JBOSS_RUNJAR%" ^
    %MODULE_OPTS% ^
    -mp "%JBOSS_MODULEPATH%" ^
    org.jboss.ws.tools.wsprovide ^
    %*

:END
