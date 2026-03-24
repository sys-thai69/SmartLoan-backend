@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME="
for /f "tokens=*" %%i in ('where java') do (
    for %%j in ("%%~dpi..") do set "JAVA_HOME=%%~fj"
    goto :found
)
:found

set "MAVEN_OPTS=-Xmx512m"

if exist ".mvn\wrapper\maven-wrapper.jar" (
    "%JAVA_HOME%\bin\java" -jar ".mvn\wrapper\maven-wrapper.jar" %*
) else (
    echo Maven wrapper JAR not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '.mvn\wrapper\maven-wrapper.jar'"
    "%JAVA_HOME%\bin\java" -jar ".mvn\wrapper\maven-wrapper.jar" %*
)

endlocal
