@echo off
SETLOCAL

rem Author: Gregory Charles, JBoss Community Member
rem Date: January 24, 2012

set "DIRNAME=%~dp0"

rem Setup JBOSS_HOME
set "JBOSS_HOME=%DIRNAME%.."

rem Setup the JVM
IF NOT DEFINED JAVA (
    IF DEFINED JAVA_HOME (
        set "JAVA=%JAVA_HOME%\bin\java"
    ) ELSE (
        set JAVA=java
    )

)

IF NOT DEFINED MODULEPATH (
    set "MODULEPATH=%JBOSS_HOME%\modules"
)

rem Display our environment
echo =========================================================================
echo.
echo   JBoss Vault
echo.
echo   JBOSS_HOME: "%JBOSS_HOME%"
echo.
echo   JAVA: "%JAVA%"
echo.
echo =========================================================================
echo.

"%JAVA%" -jar "%JBOSS_HOME%\jboss-modules.jar" -mp "%MODULEPATH%" org.jboss.as.vault-tool %*

ENDLOCAL