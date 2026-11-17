@echo off
setlocal
echo ========================================================
echo             Building ReverseCrouch 1.0.0               
echo    1.8.9 FastBridging & Snappy Crouch for Paper 26.3   
echo ========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo.
pause

