@echo off
setlocal enabledelayedexpansion

echo ============================================
echo    SmartLoan Backend Starter
echo ============================================
echo.

cd /d "%~dp0"

REM Load environment variables from .env file
echo Loading environment variables from .env...
for /f "usebackq delims== tokens=1,*" %%A in (".env") do (
    if not "%%A"=="" if not "%%A:~0,1%"=="#" (
        set "%%A=%%B"
    )
)

REM Set JAVA_HOME from java in PATH
for /f "tokens=*" %%i in ('where java 2^>nul') do (
    set "JAVA_EXE=%%i"
    goto :found_java
)
echo ERROR: Java not found in PATH
pause
exit /b 1

:found_java
echo Found Java: %JAVA_EXE%

REM Create a temp directory without spaces
set "MAVEN_HOME=%TEMP%\maven"
set "MAVEN_ZIP=%TEMP%\maven.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo.
    echo Downloading Apache Maven...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_ZIP%'"

    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%TEMP%' -Force"

    if exist "%TEMP%\apache-maven-3.9.6" (
        if exist "%MAVEN_HOME%" rmdir /s /q "%MAVEN_HOME%"
        rename "%TEMP%\apache-maven-3.9.6" "maven"
    )

    del "%MAVEN_ZIP%" 2>nul
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo ERROR: Failed to install Maven
    pause
    exit /b 1
)

echo.
echo Using Maven from: %MAVEN_HOME%
echo.
echo Starting Spring Boot application...
echo.

"%MAVEN_HOME%\bin\mvn.cmd" spring-boot:run

pause
