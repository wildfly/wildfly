@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%
set PROGNAME=run.bat
if "%OS%" == "Windows_NT" set PROGNAME=%~nx0%

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)
set JBOSS_HOME=%DIRNAME%\..

rem Execute the command
"%JAVA%" %JAVA_OPTS% ^
    "-Djava.endorsed.dirs=%JBOSS_HOME%/modules/com/sun/xml/bind/main;%JBOSS_HOME%/modules/javax/xml/ws/api/main" ^
    -classpath "%JAVA_HOME%\lib\tools.jar;%JBOSS_HOME%\jboss-modules.jar" ^
    org.jboss.modules.Main ^
    -mp "%JBOSS_HOME%\modules" ^
    org.jboss.ws.tools.wsconsume ^
    %*
