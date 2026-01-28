@echo off
echo ====================================
echo Building UnderwaterDepth Plugin
echo ====================================

call gradlew.bat clean shadowJar

if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo ====================================
echo Deploying to Hytale Mods folder
echo ====================================

copy /Y "build\libs\UnderwaterDepth-1.0.0.jar" "%APPDATA%\Hytale\UserData\Mods\"

echo.
echo ====================================
echo Installation Complete!
echo ====================================
echo Plugin installed to: %APPDATA%\Hytale\UserData\Mods\UnderwaterDepth-1.0.0.jar
echo.
pause
