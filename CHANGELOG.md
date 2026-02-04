# Changelog

## v1.0.4 (2026-02-04)

**Per-Player Settings Persistence**

### What's New
- Settings now persist to disk per-player
  - Each player's preferences saved to `player_settings.json`
  - Settings survive server restarts and player disconnects
  - No more reconfiguring every time you join

### Changed
- Command documentation now complete in CurseForge README
  - Added `/wdepth help` - Show command list and usage
  - Added `/wdepth sea <on|off>` - Toggle sea level display
  - Added `/wdepth decimal <on|off>` - Toggle decimal precision
  - Added `/wdepth credits` - Show plugin credits
  - Updated `/wdepth` (no args) - Now shows your current settings

### Technical
- Added `PlayerConfig.loadConfigs()` and `PlayerConfig.getAllConfigs()` for serialization
- Settings auto-save after each command change
- File location: `C:\Users\[USERNAME]\AppData\Roaming\Hytale\UserData\Saves\[WORLD_NAME]\mods\BeyondSmash_WaterDepthGauge\player_settings.json`

### Migration from v1.0.3
- Update the JAR and restart - existing settings will use defaults
- Configure your preferences with commands - they'll persist from now on

---

## v1.0.3

**Major Refactor: Proper MultipleHUD Integration**

### Changes
- **Removed entire compatibility system** (500+ lines of code)
  - No more `compatible_mods.json` auto-detection
  - No more `UPDATE.bat` workflow
  - No more external UI file management
- **Why?** The old system was a "dominant UI system" that overrode MultipleHUD's container, breaking compatibility with other HUD mods (reported by Blocchio developers)

### What's New
- **Proper MultipleHUD API integration**
  - Now registers via `MultipleHUD.getInstance().setCustomHud()` (standard API)
  - Lets MultipleHUD manage all UI container logic internally
  - No longer creates own `Group #MultipleHUD` container
- **Cross-mod compatibility**
  - Works with ALL MultipleHUD-compatible mods (Blocchio, BlockInfo, EyeSpyHUD, etc.)
  - No special configuration needed - just install and play

### Deleted Files
- `src/main/java/com/underwaterdepth/CompatibleModsConfig.java` (obsolete)
- `src/main/java/com/underwaterdepth/EmptyHUD.java` (obsolete)
- `UPDATE.bat` (no longer needed)
- `auto-update.ps1` (no longer needed)
