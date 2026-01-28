@echo off
echo ========================================
echo WaterDepthGauge - Auto-Update Script
echo ========================================
echo.
echo This script will:
echo 1. Read compatible_mods.json
echo 2. Add missing mods to .ui file
echo 3. Rebuild the plugin
echo 4. Deploy to Mods folder
echo.
pause

echo.
echo Running PowerShell auto-update script...
powershell.exe -ExecutionPolicy Bypass -File "%~dp0auto-update.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Auto-update failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Update complete!
echo Restart Hytale to load the updated plugin.
echo ========================================
pause
