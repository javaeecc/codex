@echo off
setlocal

cd /d "%~dp0"

set "JAVA_EXE=%~dp0jre\bin\java.exe"
set "APP_JAR=%~dp0codex-gateway.jar"

if not exist "%JAVA_EXE%" (
    echo [ERROR] Bundled JRE not found: "%JAVA_EXE%"
    pause
    exit /b 1
)

if not exist "%APP_JAR%" (
    echo [ERROR] Application jar not found: "%APP_JAR%"
    pause
    exit /b 1
)

echo Starting codex-gateway on port 80...
"%JAVA_EXE%" -jar "%APP_JAR%" --server.port=80

if errorlevel 1 (
    echo.
    echo [ERROR] codex-gateway stopped with exit code %errorlevel%.
    pause
)

endlocal
