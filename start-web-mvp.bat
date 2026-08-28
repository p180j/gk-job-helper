@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "WEB_DIR=%PROJECT_DIR%gk-job-helper-web"

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found in PATH. Please install Maven or configure PATH first.
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo Node.js and npm were not found in PATH. Please install Node.js 18+ first.
  pause
  exit /b 1
)

if not exist "%WEB_DIR%\package.json" (
  echo Frontend directory was not found: %WEB_DIR%
  pause
  exit /b 1
)

echo Starting GK Job Helper...
echo MySQL must be available at localhost:3306, database: gk_job_helper.

start "GK Job Helper Backend" cmd /k "cd /d ""%PROJECT_DIR%"" && mvn spring-boot:run"
start "GK Job Helper Frontend" cmd /k "cd /d ""%WEB_DIR%"" && if not exist node_modules (npm install) && npm run dev"

echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo Two terminal windows have been opened. Keep both running while using the application.
pause
