@echo off
echo =======================================================
echo Building Imagery Plugin (Style 26.3)...
echo =======================================================

mvn clean package

if %ERRORLEVEL% EQU 0 (
    copy /Y target\Imagery-1.0.0.jar Imagery.jar
    echo.
    echo =======================================================
    echo BUILD SUCCESS! Imagery.jar is ready in this directory.
    echo =======================================================
) else (
    echo.
    echo BUILD FAILED! Please check error output above.
)
pause

