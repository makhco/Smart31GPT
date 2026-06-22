@echo off
setlocal enabledelayedexpansion
set GRADLE_VERSION=8.10.2
set BASE_DIR=%~dp0
set DIST_DIR=%BASE_DIR%.gradle-local
set GRADLE_HOME=%DIST_DIR%\gradle-%GRADLE_VERSION%
set ZIP_FILE=%DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip
set URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
  if not exist "%ZIP_FILE%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP_FILE%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_FILE%' '%DIST_DIR%'"
  if errorlevel 1 exit /b 1
)

"%GRADLE_HOME%\bin\gradle.bat" %*
