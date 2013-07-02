@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM -------------------------------------------------------------------------
REM  Red Hat - JBoss EAP 6 Service Script for Windows
REM            It has to reside in modules\native\sbin\
REM
REM  2013-07-01 Minor changes to make it work with Wildfly
REM  2012-09-14 fixed service log path
REM             cmd line options for controller,domain host, loglevel,
REM		username,password
REM  2012-09-05 NOPAUSE support
REM  2012-08-20 initial edit
REM
REM Author: Tom Fonteyne
REM
REM ==================================================================
REM If more then one service is needed, copy this file to another name
REM and use unique names for SHORTNAME, DISPLAYNAME, DESCRIPTION
REM ==================================================================
set SHORTNAME=Wildfly
set DISPLAYNAME="Wildfly"
set DESCRIPTION="Wildfly Application Server"
REM ========================================================

set "DIRNAME=%~dp0%"
cd > nul
pushd %DIRNAME%..\..
set "RESOLVED_JBOSS_HOME=%CD%"
popd
set DIRNAME=

if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%" 
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
    echo WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
    echo Please check that this script is located in the bin\services\ directory.
    goto cmdEnd
)
rem Find jboss-modules.jar to check JBOSS_HOME
if not exist "%JBOSS_HOME%\jboss-modules.jar" (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that this script is located in the bin\services\ directory.
  goto cmdEnd
)

if "x%JAVA_HOME%" == "x" (
  echo JAVA_HOME is not set.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
  goto cmdEnd
)

if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  echo Using the X86-64bit version of prunsrv
  set PRUNSRV="%JBOSS_HOME%\bin\service\amd64\wildfly-service"
) else (
  echo Using the X86-32bit version of prunsrv
  set PRUNSRV="%JBOSS_HOME%\bin\service\wildfly-service"
)

if /I "%1" == "install"   goto cmdInstall
if /I "%1" == "uninstall" goto cmdUninstall
if /I "%1" == "start"     goto cmdStart
if /I "%1" == "stop"      goto cmdStop
if /I "%1" == "restart"   goto cmdRestart

:cmdUsage
echo Usage:
echo   service install [/controller localhost:9990] [/loglevel INFO] [/host [domainhost]] [/user username /password password] 
echo   service uninstall
echo   service start
echo   service stop
echo   service restart
echo(
echo The options for "service install":
echo   /controller        : the host:port of the domain controller, default: 127.0.0.1:9990
echo   /loglevel          : the log level for the service:  Error, Info, Warn or Debug ^(Case insensitive^), default: INFO 
echo   /host [domainhost] : indicated domain mode is to be used with an optional domain controller name, default: master
echo                        Not specifying /host will install in standalone mode
echo   /user              : username for the shutdown command
echo   /password          : password for the shutdown command
echo(
goto endBatch

:cmdInstall
shift

set CONTROLLER=localhost:9990
set DC_HOST=master
set IS_DOMAIN=false
set LOGLEVEL=INFO
set JBOSSUSER=
set PASSWORD=

:LoopArgs
if "%1"=="" goto doInstall

if /I "%1"=="/controller" (
  if not "%2"=="" (
    rem should really check if the format is acceptable
    set CONTROLLER=%2
  ) else (
    echo ERROR: A controller url should be specified in the format host:port, example:  127.0.0.1:9990
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%1"=="/user" (
  if not "%2"=="" (
    set JBOSSUSER=%2
  ) else (
    echo ERROR: You need to specify a username
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%1"=="/password" (
  if not "%2"=="" (
    set PASSWORD=%2
  ) else (
    echo ERROR: You need to specify a password
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%1"=="/host" (
  set IS_DOMAIN=true
  if not "%2"=="" (
    set T=%2
    if not "!T:~0,1!"=="/" (
      set DC_HOST=!T!
      shift
    )
  )
  shift
  goto LoopArgs
)
if /I "%1"=="/loglevel" (
  if /I not "%2"=="Error" if /I not "%2"=="Info" if /I not "%2"=="Warn" if /I not "%2"=="Debug" (
    echo ERROR: /loglevel must be set to Error, Info, Warn or Debug ^(Case insensitive^)
    goto endBatch      
  )
  set LOGLEVEL=%2
  shift
  shift
  goto LoopArgs
)
echo ERROR: Unrecognised option: %1
echo(
goto cmdUsage

:doInstall
set CREDENTIALS=
if not "%JBOSSUSER%" == "" (
  if "%PASSWORD%" =="" (
    echo When specifying a user, you need to specify the password
    goto endBatch
  )
  set CREDENTIALS=--user=%JBOSSUSER% --password=%PASSWORD%
)
if /I "%IS_DOMAIN%" == "true" (
  set STARTPARAM="/c \"set NOPAUSE=Y ^^^&^^^& domain.bat\""
  set STOPPARAM="/c jboss-cli.bat --controller=%CONTROLLER% --connect %CREDENTIALS% --command=/host=!DC_HOST!:shutdown"
  set LOGPATH=%JBOSS_HOME%\domain\log
) else (
  set STARTPARAM="/c \"set NOPAUSE=Y ^^^&^^^& standalone.bat\""
  set STOPPARAM="/c \"set NOPAUSE=Y ^^^&^^^& jboss-cli.bat --controller=%CONTROLLER% --connect %CREDENTIALS% --command=:shutdown\""
  set LOGPATH=%JBOSS_HOME%\standalone\log
)

rem echo %STARTPARAM%
rem echo %STOPPARAM%
rem echo %LOGLEVEL%
rem goto endBatch

%PRUNSRV% install %SHORTNAME% --DisplayName=%DISPLAYNAME% --Description %DESCRIPTION% --LogLevel=%LOGLEVEL% --LogPath="%LOGPATH%" --LogPrefix=service --StdOutput=auto --StdError=auto --StartMode=exe --StartImage=cmd.exe --StartPath="%JBOSS_HOME%\bin" ++StartParams=%STARTPARAM% --StopMode=exe --StopImage=cmd.exe --StopPath="%JBOSS_HOME%\bin"  ++StopParams=%STOPPARAM%
goto cmdEnd

:cmdUninstall
%PRUNSRV% stop %SHORTNAME%
if "%errorlevel%" == "0" (
  %PRUNSRV% delete %SHORTNAME%
) else (
  echo Unable to stop the service
)
goto cmdEnd

:cmdStart
%PRUNSRV% start %SHORTNAME%
goto cmdEnd

:cmdStop
%PRUNSRV% stop %SHORTNAME%
goto cmdEnd

:cmdRestart
%PRUNSRV% stop %SHORTNAME%
if "%errorlevel%" == "0" (
  %PRUNSRV% start %SHORTNAME%
) else (
  echo Unable to stop the service
)
goto cmdEnd

:cmdEnd
REM need to add other error messages (list higher nr first !)
if errorlevel 8 (
  echo ERROR: The service %SHORTNAME% already exists
  goto endBatch
)
if errorlevel 2 (
  echo ERROR: Failed to load service configuration
  goto endBatch
)
if errorlevel 0 (
  echo Success
  goto endBatch
)
echo errorlevel=%errorlevel%

rem nothing below, exit
:endBatch
