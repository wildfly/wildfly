@ECHO OFF
cd ../..
cd %cd%/dist/target/wildfly-*


SET JBOSS_FOLDER=%cd%
echo %JBOSS_FOLDER%
for %%f in (%JBOSS_FOLDER%) do set WILDFLY_FOLDER=%%~nxf

SET JBOSS_VERSION=%WILDFLY_FOLDER:~8%
echo %JBOSS_VERSION%

cd ../../../testsuite/additional-testsuite/eap-additional-testsuite

if exist /store/repository (
    call mvn clean install -Dwildfly -Dwildfly-jdk8 -Dserver-integration -Dmaven.repo.local=/store/repository > ../output.txt
) else (
    call mvn clean install -Dwildfly -Dwildfly-jdk8 -Dserver-integration > ../output.txt
)

cd ..

type output.txt

if exist errors.txt (
    del errors.txt
)

findstr /c:"ERROR" output.txt

if %errorlevel% EQU 0 goto :errors_exist

echo Eap Additional Testsuite was completed successfully ...

exit

:errors_exist
    (echo Eap Additional Testsuite was completed with errors ...) > errors.txt
    (echo BUILD ERRORS ...) >> errors.txt
    (findstr /c:"ERROR" output.txt) >> errors.txt
  
    echo ,> error_lines.txt
    for /f "delims=" %%a in ('findstr /n /r ".*FAILURE!.-" output.txt') do (
        setlocal enabledelayedexpansion
        for /f "tokens=1 delims=:" %%b in ("%%a") do (
            set lineNum=%%b
            set /A lines=!lineNum!
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 1
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 2
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 3
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 4
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 5
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 6
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 7
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 8
            echo !lines!,>> error_lines.txt
            set /a lines = !lineNum! + 9
            echo !lines!,>> error_lines.txt
        )
    )
   echo ERRORS WHERE DETECTED ... >> errors.txt
   for /f "tokens=1* delims=:" %%a in ('findstr /n .* "output.txt"') do (
        for /f "tokens=1 delims=," %%c in (error_lines.txt) do (
            if "%%a" equ "%%c" echo.%%b >> errors.txt
        )
   )

    echo Eap Additional Testsuite was completed with errors ...
   
    exit
