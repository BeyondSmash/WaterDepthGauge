# WaterDepthGauge Auto-Update Script
# Automatically adds missing compatible mods to .ui file and rebuilds

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "WaterDepthGauge Auto-Update" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = "$env:APPDATA\Hytale\UserData\Mods\WaterDepthGauge\compatible_mods.json"
$uiFilePath = "$scriptDir\src\main\resources\Common\UI\Custom\Hud\UnderwaterDepth\UnderwaterDepth_DepthMeter.ui"
$modsFolder = "$env:APPDATA\Hytale\UserData\Mods"

# Step 1: Read compatible_mods.json
Write-Host "[1/5] Reading compatible_mods.json..." -ForegroundColor Yellow

if (-not (Test-Path $configPath)) {
    Write-Host "ERROR: Config file not found at: $configPath" -ForegroundColor Red
    Write-Host "Make sure the plugin has been run at least once to generate the config." -ForegroundColor Red
    exit 1
}

$config = Get-Content $configPath -Raw | ConvertFrom-Json
$allMods = $config.compatibleHudMods | Where-Object { $_.modId -ne "_DEFAULT" }

Write-Host "Found $($allMods.Count) mods in config (excluding _DEFAULT)" -ForegroundColor Green

# Step 2: Read .ui file
Write-Host ""
Write-Host "[2/5] Reading .ui file..." -ForegroundColor Yellow

if (-not (Test-Path $uiFilePath)) {
    Write-Host "ERROR: UI file not found at: $uiFilePath" -ForegroundColor Red
    exit 1
}

$uiContent = Get-Content $uiFilePath -Raw

# Step 3: Find missing mods
Write-Host ""
Write-Host "[3/5] Checking for missing mods..." -ForegroundColor Yellow

$missingMods = @()
foreach ($mod in $allMods) {
    $modId = $mod.modId
    $searchPattern = "Group #$modId"

    if ($uiContent -notmatch [regex]::Escape($searchPattern)) {
        $missingMods += $mod
        Write-Host "  - Found missing mod: $modId ($($mod.width)x$($mod.height))" -ForegroundColor Cyan
    }
}

if ($missingMods.Count -eq 0) {
    Write-Host "No missing mods found - .ui file is up to date!" -ForegroundColor Green
    Write-Host ""
    Write-Host "[4/5] Building plugin anyway..." -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "Found $($missingMods.Count) missing mod(s) to add" -ForegroundColor Yellow

    # Step 4: Add missing mods to .ui file
    Write-Host ""
    Write-Host "[4/5] Adding missing mods to .ui file..." -ForegroundColor Yellow

    # Find the insertion point (before #UnderwaterDepth)
    $insertionMarker = "  Group #UnderwaterDepth {"
    $insertionIndex = $uiContent.IndexOf($insertionMarker)

    if ($insertionIndex -eq -1) {
        Write-Host "ERROR: Could not find insertion point in .ui file" -ForegroundColor Red
        Write-Host "Looking for: $insertionMarker" -ForegroundColor Red
        exit 1
    }

    # Generate new Groups
    $newGroups = ""
    foreach ($mod in $missingMods) {
        $newGroups += "  Group #$($mod.modId) {`n"
        $newGroups += "    Anchor: (Left: 0, Top: 0, Width: $($mod.width), Height: $($mod.height));`n"
        $newGroups += "  }`n"
        Write-Host "  + Added: Group #$($mod.modId)" -ForegroundColor Green
    }

    # Insert new Groups
    $before = $uiContent.Substring(0, $insertionIndex)
    $after = $uiContent.Substring($insertionIndex)
    $newUiContent = $before + $newGroups + $after

    # Save updated .ui file
    Set-Content -Path $uiFilePath -Value $newUiContent -NoNewline
    Write-Host ""
    Write-Host "Successfully updated .ui file!" -ForegroundColor Green
}

# Step 5: Build plugin
Write-Host ""
Write-Host "[5/5] Building plugin..." -ForegroundColor Yellow

$gradlewPath = Join-Path $scriptDir "gradlew.bat"
Push-Location $scriptDir

try {
    & $gradlewPath clean shadowJar 2>&1 | ForEach-Object {
        if ($_ -match "BUILD SUCCESSFUL") {
            Write-Host $_ -ForegroundColor Green
        } elseif ($_ -match "BUILD FAILED") {
            Write-Host $_ -ForegroundColor Red
        } elseif ($_ -match "^>") {
            Write-Host $_ -ForegroundColor Gray
        }
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE"
    }

    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green

    # Deploy to Mods folder
    Write-Host ""
    Write-Host "Deploying to Mods folder..." -ForegroundColor Yellow

    # Find the built JAR (version-agnostic)
    $jarPath = Get-ChildItem -Path (Join-Path $scriptDir "build\libs") -Filter "WaterDepthGauge-*.jar" | Select-Object -First 1 -ExpandProperty FullName
    if (-not $jarPath) {
        throw "Built JAR not found in build/libs/"
    }

    $jarName = Split-Path $jarPath -Leaf
    Copy-Item -Path $jarPath -Destination $modsFolder -Force
    Write-Host "Deployed $jarName to: $modsFolder" -ForegroundColor Green

} catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Pop-Location
    exit 1
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Update complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "  - Added $($missingMods.Count) new mod(s) to .ui file" -ForegroundColor White
Write-Host "  - Built and deployed $jarName" -ForegroundColor White
Write-Host ""
Write-Host "Restart Hytale to load the updated plugin." -ForegroundColor Yellow

exit 0
