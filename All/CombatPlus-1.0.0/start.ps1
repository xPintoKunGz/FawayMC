# Minecraft 26.3 Server Runner (PowerShell) - Made By FawayMC
$Host.UI.RawUI.WindowTitle = "Minecraft 26.3 Server Runner - CombatPlus (Made By FawayMC)"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       Minecraft 26.3 Server Runner with CombatPlus      " -ForegroundColor Yellow
Write-Host "                   Made By FawayMC                      " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# Find Java
$javaCmd = "java"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $candidates = @(
        "C:\Program Files\Android\Android Studio\jbr\bin\java.exe",
        "C:\Program Files\Java\jdk-25\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe",
        "C:\Program Files\Java\jdk-20\bin\java.exe"
    )
    $found = $false
    foreach ($cand in $candidates) {
        if (Test-Path $cand) {
            $javaCmd = $cand
            $found = $true
            break
        }
    }
    if (-not $found) {
        Write-Host "[ERROR] Java 21/25 not found!" -ForegroundColor Red
        Write-Host "Please install Java from https://adoptium.net/" -ForegroundColor Yellow
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

Write-Host "[*] Using Java: $javaCmd" -ForegroundColor Green
& $javaCmd -version
Write-Host ""

# Find Server Jar
$serverJar = $null
if (Test-Path "paper.jar") { $serverJar = "paper.jar" }
elseif (Test-Path "server.jar") { $serverJar = "server.jar" }
else {
    $jars = Get-ChildItem -Filter "paper-*.jar"
    if ($jars.Count -gt 0) { $serverJar = $jars[0].Name }
}

if (-not $serverJar) {
    Write-Host "[!] No server jar found. Downloading Paper 26.3..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri "https://api.papermc.io/v2/projects/paper/versions/26.3/builds/227/downloads/paper-26.3-227.jar" -OutFile "paper.jar" -UseBasicParsing
        $serverJar = "paper.jar"
        Write-Host "[*] Downloaded paper.jar successfully!" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Could not download server jar. Please place paper.jar in this folder." -ForegroundColor Red
        Read-Host "Press Enter to exit..."
        exit 1
    }
}

# Ensure plugins folder and copy plugin
if (-not (Test-Path "plugins")) {
    New-Item -ItemType Directory -Path "plugins" | Out-Null
}
if (Test-Path "CombatPlus-1.0.0.jar") {
    Copy-Item -Path "CombatPlus-1.0.0.jar" -Destination "plugins\CombatPlus-1.0.0.jar" -Force
    Write-Host "[*] CombatPlus-1.0.0.jar installed in plugins/" -ForegroundColor Green
}

# Check EULA
if (-not (Test-Path "eula.txt") -or (Get-Content "eula.txt" -Raw) -notmatch "eula=true") {
    Set-Content -Path "eula.txt" -Value "eula=true"
    Write-Host "[*] eula.txt set to true" -ForegroundColor Green
}

$minRam = "2G"
$maxRam = "4G"

$jvmFlags = @(
    "-Xms$minRam",
    "-Xmx$maxRam",
    "-XX:+UseG1GC",
    "-XX:+ParallelRefProcEnabled",
    "-XX:MaxGCPauseMillis=200",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:+DisableExplicitGC",
    "-XX:+AlwaysPreTouch",
    "-XX:G1NewSizePercent=30",
    "-XX:G1MaxNewSizePercent=40",
    "-XX:G1ReservePercent=20",
    "-XX:G1HeapWastePercent=5",
    "-XX:G1MixedGCCountTarget=4",
    "-XX:InitiatingHeapOccupancyPercent=15",
    "-XX:G1MixedGCLiveThresholdPercent=90",
    "-XX:G1RSetUpdatingPauseTimePercent=5",
    "-XX:SurvivorRatio=32",
    "-XX:+PerfDisableSharedMem",
    "-XX:MaxTenuringThreshold=1",
    "-Dusing.aikars.flags=https://mcflags.emc.gs",
    "-Daikars.new.flags=true"
)

while ($true) {
    Write-Host "`n[*] Starting Minecraft 26.3 server ($serverJar) - Made By FawayMC..." -ForegroundColor Cyan
    & $javaCmd $jvmFlags -jar $serverJar --nogui
    Write-Host "`n[!] Server stopped. Restarting in 5 seconds (Press Ctrl+C to cancel)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}
