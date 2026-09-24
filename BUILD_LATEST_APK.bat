@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo   MY CHHACHH - LOCAL APK BUILD
echo ==========================================
echo.

if not exist gradlew.bat (
  echo ERROR: gradlew.bat not found.
  pause
  exit /b 1
)

call gradlew.bat --no-daemon clean assembleDebug
if errorlevel 1 (
  echo.
  echo BUILD FAILED. See the error above.
  pause
  exit /b 1
)

set "SRC=app\build\outputs\apk\debug\app-debug.apk"
set "OUT=MY_CHHACHH_LATEST_DEBUG.apk"

if not exist "%SRC%" (
  echo.
  echo ERROR: APK was not found at %SRC%
  pause
  exit /b 1
)

copy /Y "%SRC%" "%OUT%" >nul
echo.
echo SUCCESS
echo APK: %CD%\%OUT%
echo.
pause
