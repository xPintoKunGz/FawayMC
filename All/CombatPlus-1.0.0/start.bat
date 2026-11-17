@echo off
setlocal enabledelayedexpansion
title Minecraft 26.3 Server Runner - CombatPlus (Made By FawayMC)

echo ========================================================
echo        Minecraft 26.3 Server Runner with CombatPlus
echo                    Made By FawayMC
echo ========================================================
echo.

:: 1. Find Java 21+ or Java 25
set "JAVA_CMD=java"
where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    goto :JAVA_FOUND
)

:: Check common JDK paths
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
    goto :JAVA_FOUND
)

if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-25\bin\java.exe"
    goto :JAVA_FOUND
)

if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-21\bin\java.exe"
    goto :JAVA_FOUND
)

if exist "C:\Program Files\Java\jdk-20\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-20\bin\java.exe"
    goto :JAVA_FOUND
)

echo [ERROR] Java was not found in PATH or standard installation directories.
echo Please install Java 21+ or Java 25 (required for Minecraft 26.3).
echo Download from: https://adoptium.net/
pause
exit /b 1

:JAVA_FOUND
echo [*] Using Java: "!JAVA_CMD!"
"!JAVA_CMD!" -version
echo.

:: 2. Find Server Jar file
set "SERVER_JAR="
if exist "paper.jar" set "SERVER_JAR=paper.jar"
if not defined SERVER_JAR if exist "server.jar" set "SERVER_JAR=server.jar"
if not defined SERVER_JAR (
    for %%f in (paper-*.jar) do (
        set "SERVER_JAR=%%f"
        goto :JAR_FOUND
    )
)

:JAR_FOUND
if not defined SERVER_JAR (
    echo [!] No server jar (paper.jar or server.jar) found in this directory.
    echo [*] Attempting to download Paper 26.3 server jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://api.papermc.io/v2/projects/paper/versions/26.3/builds/227/downloads/paper-26.3-227.jar' -OutFile 'paper.jar' -UseBasicParsing" 2>nul
    if exist "paper.jar" (
        set "SERVER_JAR=paper.jar"
        echo [*] Downloaded paper.jar successfully!
    ) else (
        echo [!] Could not auto-download server jar. Please place your paper.jar or server.jar in this folder.
        pause
        exit /b 1
    )
)

echo [*] Server jar identified: "!SERVER_JAR!"

:: 3. Automatically ensure CombatPlus is in plugins folder
if not exist "plugins" mkdir plugins
if exist "CombatPlus-1.0.0.jar" (
    copy /Y "CombatPlus-1.0.0.jar" "plugins\CombatPlus-1.0.0.jar" >nul
    echo [*] CombatPlus-1.0.0.jar copied to plugins/ directory.
)

:: 4. Check eula.txt
if not exist "eula.txt" (
    echo eula=true> eula.txt
    echo [*] Created eula.txt (eula=true).
) else (
    findstr /I "eula=true" eula.txt >nul
    if %ERRORLEVEL% neq 0 (
        echo eula=true> eula.txt
        echo [*] Updated eula.txt to eula=true.
    )
)

:: 5. RAM configuration (Default 4GB, adjust as needed)
set "MIN_RAM=2G"
set "MAX_RAM=4G"

:: Optimized Aikar JVM flags for Paper 26.3
set "JVM_FLAGS=-Xms!MIN_RAM! -Xmx!MAX_RAM! -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true"

:SERVER_LOOP
echo.
echo ========================================================
echo        Starting Server (!SERVER_JAR!) with !MAX_RAM! RAM
echo                    Made By FawayMC
echo ========================================================
echo.

"!JAVA_CMD!" !JVM_FLAGS! -jar "!SERVER_JAR!" --nogui

echo.
echo [!] Server stopped.
echo Restarting in 5 seconds... Press Ctrl+C to stop.
timeout /t 5 /nobreak >nul
goto :SERVER_LOOP
