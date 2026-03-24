@echo off
echo Starting SmartLoan Backend...
echo.

cd /d "%~dp0"

REM Check if target folder exists (already compiled)
if exist "target\smartloan-backend-0.0.1-SNAPSHOT.jar" (
    echo Running pre-built JAR...
    java -jar "target\smartloan-backend-0.0.1-SNAPSHOT.jar"
) else (
    echo First run - need to build. Opening in IDE is recommended.
    echo.
    echo Options:
    echo 1. Open backend folder in IntelliJ IDEA or VS Code
    echo 2. Install Maven: https://maven.apache.org/download.cgi
    echo 3. Use Docker: docker-compose up backend
    echo.
    pause
)
