@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set "DIRNAME=%~dp0%"
set PROGNAME=run.bat
if "%OS%" == "Windows_NT" set "PROGNAME=%~nx0%"

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)
set "JBOSS_HOME=%DIRNAME%\.."

set "JAVA_OPTS=%JAVA_OPTS% -Dprogram.name=wsconsume.bat"

rem Execute the command
"%JAVA%" %JAVA_OPTS% ^
    -classpath "%JAVA_HOME%\lib\tools.jar;%JBOSS_HOME%\jboss-modules.jar" ^
    org.jboss.modules.Main ^
    -mp "%JBOSS_HOME%\modules" ^
    org.jboss.ws.tools.wsconsume ^
    %*
