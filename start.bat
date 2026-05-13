@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo STARTING ELIXIR / EXPRESS ELIXIR / SORBNET
echo ==========================================
echo.

echo Checking Docker CLI...
docker --version >nul 2>nul
if errorlevel 1 (
    echo Docker is not installed.
    pause
    exit /b 1
)

echo Checking Docker Engine...
docker info >nul 2>nul
if errorlevel 1 (
    echo Docker Desktop is not running or Docker Engine is unavailable.
    pause
    exit /b 1
)

echo.
echo Cleaning old stack...
docker compose down -v --remove-orphans >nul 2>nul

echo.
echo Starting stack...
docker compose up -d --build
if errorlevel 1 (
    echo.
    echo Docker Compose failed to start the stack.
    echo.
    echo ==== STATUS ====
    docker compose ps -a
    echo.
    echo ==== YUGABYTE LOGS ====
    docker compose logs --tail=120 yugabyte
    echo.
    echo ==== KAFKA INIT LOGS ====
    docker compose logs --tail=120 kafka-init
    echo.
    echo ==== SORBNET LOGS ====
    docker compose logs --tail=120 sorbnet_system
    pause
    exit /b 1
)

echo.
echo Waiting 20 seconds for healthchecks...
timeout /t 20 /nobreak >nul

echo.
echo ==== STATUS ====
docker compose ps -a

echo.
echo ==== YUGABYTE LOGS ====
docker compose logs --tail=80 yugabyte

echo.
echo ==== KAFKA INIT LOGS ====
docker compose logs --tail=80 kafka-init

echo.
echo ==== SORBNET LOGS ====
docker compose logs --tail=80 sorbnet_system

echo.
echo Done.
pause
exit /b 0