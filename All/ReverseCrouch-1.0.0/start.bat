@echo off
setlocal enabledelayedexpansion
title Minecraft 26.3 Server Runner - ReverseCrouch

echo ========================================================
echo       Minecraft 26.3 Server Runner with ReverseCrouch
echo          1.8.9 FastBridging & Snappy Crouch
echo ========================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
if %ERRORLEVEL% NEQ 0 (
    pause
    exit /b %ERRORLEVEL%
)

