#!/usr/bin/env bash
# Minecraft 26.3 Server Runner - CombatPlus (Made By FawayMC)
echo "========================================================"
echo "       Minecraft 26.3 Server Runner with CombatPlus      "
echo "                   Made By FawayMC                      "
echo "========================================================"

# Find Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH."
    echo "Please install Java 21+ or Java 25."
    exit 1
fi

# Find server jar
SERVER_JAR=""
if [ -f "paper.jar" ]; then
    SERVER_JAR="paper.jar"
elif [ -f "server.jar" ]; then
    SERVER_JAR="server.jar"
else
    SERVER_JAR=$(ls paper-*.jar 2>/dev/null | head -n 1)
fi

if [ -z "$SERVER_JAR" ]; then
    echo "[!] No server jar found. Downloading Paper 26.3..."
    curl -o paper.jar -L "https://api.papermc.io/v2/projects/paper/versions/26.3/builds/227/downloads/paper-26.3-227.jar"
    SERVER_JAR="paper.jar"
fi

# Ensure plugin is copied to plugins/
mkdir -p plugins
if [ -f "CombatPlus-1.0.0.jar" ]; then
    cp -f "CombatPlus-1.0.0.jar" "plugins/CombatPlus-1.0.0.jar"
    echo "[*] CombatPlus-1.0.0.jar installed in plugins/ (Made By FawayMC)"
fi

# EULA
echo "eula=true" > eula.txt

MIN_RAM="2G"
MAX_RAM="4G"
JVM_FLAGS="-Xms$MIN_RAM -Xmx$MAX_RAM -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true"

while true; do
    echo ""
    echo "[*] Starting Server ($SERVER_JAR) - Made By FawayMC..."
    java $JVM_FLAGS -jar "$SERVER_JAR" --nogui
    echo ""
    echo "[!] Server stopped. Restarting in 5 seconds (Press Ctrl+C to cancel)..."
    sleep 5
done
