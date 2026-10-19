@echo off
setlocal
title Start MQTT broker and IoT backend
set "PROJECT_DIR=%~dp0"

echo [1/2] Checking Mosquitto broker service...
sc query mosquitto | findstr /I "RUNNING" >nul
if errorlevel 1 (
    echo Starting Mosquitto service...
    net start mosquitto
    if errorlevel 1 (
        echo.
        echo Could not start Mosquitto. Right-click this file and choose Run as administrator,
        echo then run it again. If the broker is already running, continue to the backend step.
        pause
        exit /b 1
    )
) else (
    echo Mosquitto is already running.
)

echo.
echo [2/2] Cleaning old Java build files and starting backend...
echo Keep this window open while using the backend.
set "IOT_PROJECT_DIR=%PROJECT_DIR%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $root=$env:IOT_PROJECT_DIR; $file=Join-Path $root 'HW\secrets.h'; if (-not (Test-Path -LiteralPath $file)) { throw 'HW\secrets.h was not found.' }; $s=Get-Content -LiteralPath $file -Raw; $q=[char]34; $pattern='MQTT_USERNAME\s*=\s*'+$q+'([^'+$q+']+)'+$q; $u=[regex]::Match($s, $pattern).Groups[1].Value; $pattern='MQTT_PASSWORD\s*=\s*'+$q+'([^'+$q+']+)'+$q; $p=[regex]::Match($s, $pattern).Groups[1].Value; if (-not $u -or -not $p) { throw 'MQTT_USERNAME or MQTT_PASSWORD is missing in HW\secrets.h.' }; $env:MQTT_BROKER_URL='tcp://127.0.0.1:2205'; $env:MQTT_USERNAME=$u; $env:MQTT_PASSWORD=$p; Set-Location (Join-Path $root 'BE'); & .\mvnw.cmd clean spring-boot:run"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Backend stopped with exit code %EXIT_CODE%.
    pause
)
exit /b %EXIT_CODE%
