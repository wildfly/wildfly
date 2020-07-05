@echo off
rem -------------------------------------------------------------------------
rem jconsole script for Windows
rem -------------------------------------------------------------------------
rem
rem A script for running jconsole with the remoting-jmx libraries on the classpath.

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
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

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  echo JAVA_HOME is not set. Unable to locate the jars needed to run jconsole.
  goto END
)

rem Setup The Classpath

set "CLASSPATH=%JAVA_HOME%\lib\jconsole.jar"
set "CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar"
set "CLASSPATH=%CLASSPATH%;%JBOSS_HOME%\bin\client\jboss-cli-client.jar"

rem echo %CLASSPATH%

"%JAVA_HOME%\bin\java.exe" --add-modules=java.se -version >nul 2>&1 && (set MODULAR_JDK=true) || (set MODULAR_JDK=false)

if "%MODULAR_JDK%" == "true" (
 if "%*" == "" (
    "%JAVA_HOME%\bin\jconsole.exe" "-J--add-modules=jdk.unsupported" "-J-Djava.class.path=%CLASSPATH%"
 ) else (
    "%JAVA_HOME%\bin\jconsole.exe" "-J--add-modules=jdk.unsupported" "-J-Djava.class.path=%CLASSPATH%" %*
 )   
) else (
 if "%*" == "" (
    "%JAVA_HOME%\bin\jconsole.exe" "-J-Djava.class.path=%CLASSPATH%"
 ) else (
    "%JAVA_HOME%\bin\jconsole.exe" "-J-Djava.class.path=%CLASSPATH%" %*
 )
)

:END
goto :EOF

:SearchForJars
pushd %1
for %%j in (*.jar) do call :ClasspathAdd %%j
popd
goto :EOF

:ClasspathAdd
set "CLASSPATH=%CLASSPATH%;%CD%\%1"
goto :EOF

:EOF
