@echo off
echo ==========================================
echo STARTING ELIXIR / EXPRESS ELIXIR / SORBNET
echo ==========================================
echo.

echo Checking Docker...
docker --version
if %errorlevel% neq 0 (
    echo Docker is not installed or Docker Desktop is not running.
    pause
    exit /b
)

echo.
echo Starting containers...
docker compose up --build

pause