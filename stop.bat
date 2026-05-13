@echo off
setlocal
cd /d "%~dp0"

echo Stopping Docker Compose services...
docker compose down --remove-orphans

if errorlevel 1 (
    echo.
    echo Failed to stop Docker Compose services.
    pause
    exit /b 1
)

echo.
echo Done.
pause
exit /b 0