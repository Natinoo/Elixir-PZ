@echo off
cd /d "%~dp0"

echo Stopping Docker Compose services...
docker compose down

echo.
echo Done.
pause