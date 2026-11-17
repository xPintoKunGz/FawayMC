# ReverseCrouch Build Script (PowerShell)
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "            Building ReverseCrouch 1.0.0               " -ForegroundColor Yellow
Write-Host "   1.8.9 FastBridging & Snappy Crouch for Paper 26.3   " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$jdkCandidates = @(
    "C:\Users\User\.antigravity-ide\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64\bin",
    "C:\Program Files\Java\jdk-25\bin",
    "C:\Program Files\Java\jdk-21\bin",
    "C:\Program Files\Java\jdk-20\bin"
)

$binDir = $null
foreach ($cand in $jdkCandidates) {
    if (Test-Path "$cand\javac.exe") {
        $binDir = $cand
        break
    }
}

if (-not $binDir) {
    Write-Host "[ERROR] Could not find a compatible JDK compiler (javac.exe)!" -ForegroundColor Red
    exit 1
}

$javac = "$binDir\javac.exe"
$jar = "$binDir\jar.exe"

Write-Host "[*] Using JDK: $binDir" -ForegroundColor Green

$rootDir = $PSScriptRoot
$srcDir = "$rootDir\src\main\java"
$resDir = "$rootDir\src\main\resources"
$libDir = "$rootDir\lib"
$targetDir = "$rootDir\target"
$classesDir = "$targetDir\classes"
$outJar = "$rootDir\ReverseCrouch-1.0.0.jar"

if (-not (Test-Path $classesDir)) {
    New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
}

$libs = (Get-ChildItem "$libDir\*.jar" | Select-Object -ExpandProperty FullName) -join ";"
$sources = (Get-ChildItem "$srcDir\*.java" -Recurse | Select-Object -ExpandProperty FullName)

Write-Host "[*] Compiling Java source files..." -ForegroundColor Cyan
& $javac -encoding UTF-8 -cp $libs -d $classesDir $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[*] Copying resources (plugin.yml, config.yml)..." -ForegroundColor Cyan
Copy-Item -Path "$resDir\*" -Destination $classesDir -Recurse -Force

Write-Host "[*] Packaging $outJar..." -ForegroundColor Cyan
Push-Location $classesDir
try {
    & $jar cvf $outJar * | Out-Null
} finally {
    Pop-Location
}

if (Test-Path $outJar) {
    $size = (Get-Item $outJar).Length
    Write-Host ""
    Write-Host "========================================================" -ForegroundColor Green
    Write-Host "  SUCCESS: Built ReverseCrouch-1.0.0.jar ($size bytes)  " -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Failed to create jar file." -ForegroundColor Red
}

