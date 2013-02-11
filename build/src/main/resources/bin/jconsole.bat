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
  set "PROGNAME=jdr.bat"
)

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  echo JAVA_HOME is not set. Unable to locate the jars needed to run jconsole.
  goto END
)

rem Find jboss-modules.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

rem Setup The Classpath

set CLASSPATH=%JAVA_HOME%\lib\jconsole.jar
set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar

call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\remoting-jmx\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\remoting3\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\logging\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\xnio\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\xnio\nio\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\sasl\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\marshalling\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\marshalling\river\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\as\cli\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\staxmapper\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\as\protocol\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\dmr\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\as\controller-client\main"
call :SearchForJars "%JBOSS_MODULEPATH%\system\layers\base\org\jboss\threads\main"

rem echo %CLASSPATH%

"%JAVA_HOME%\bin\jconsole.exe" -J"-Djava.class.path=%CLASSPATH%"

:END
goto :EOF

:SearchForJars
set NEXT_MODULE_DIR=%1
call :DeQuote NEXT_MODULE_DIR
pushd %NEXT_MODULE_DIR%
for %%j in (*.jar) do call :ClasspathAdd "%NEXT_MODULE_DIR%\%%j"
popd
goto :EOF

:ClasspathAdd
set NEXT_JAR=%1
call :DeQuote NEXT_JAR
set CLASSPATH=%CLASSPATH%;%NEXT_JAR%
goto :EOF

:DeQuote
for /f "delims=" %%A in ('echo %%%1%%') do set %1=%%~A
goto :EOF

:EOF
