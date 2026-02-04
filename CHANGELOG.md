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

## v1.0.3 (2026-01-31)

**Major Refactor: Proper MultipleHUD Integration**

### Breaking Changes
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

### Technical Changes
- UI Group renamed: `#UnderwaterDepth` → `#WaterDepthGauge` (matches normalized mod ID)
- Removed manual `hud.show()` call after MultipleHUD registration (was causing double-show bug)
- Mod ID format: `"WaterDepthGauge"` (no special characters to avoid normalization issues)
- Added `$Common = "Common.ui";` import to UI file (MultipleHUD pattern)

### Deleted Files
- `src/main/java/com/underwaterdepth/CompatibleModsConfig.java` (obsolete)
- `src/main/java/com/underwaterdepth/EmptyHUD.java` (obsolete)
- `UPDATE.bat` (no longer needed)
- `auto-update.ps1` (no longer needed)

### Migration from v1.0.2
- **No action required** - Just update the JAR and restart
- Plugin data location remains unchanged: `%AppData%\Roaming\Hytale\UserData\Saves\[WORLD_NAME]\mods\BeyondSmash_WaterDepthGauge\`

---

## v1.0.2 (2026-01-28)

**Major Update: Automated MultipleHUD Compatibility System**

### What's New
- **Auto-Detection System**: Plugin now automatically detects incompatible MultipleHUD mods from disconnect errors
- **UPDATE.bat Workflow**: One-click fix for compatibility issues - no manual .ui editing required
- **Pre-configured Mods**: Now includes support for BlockInfo, AdvancedItemInfo, AdminUI, and EyeSpyHUD out of the box

### Technical Improvements
- Improved mod ID parsing with better edge case handling (newlines, spaces, special characters)
- Added `[AUTO-DETECT]` debug logging for troubleshooting
- Version-agnostic JAR detection in build scripts
- Auto-saves detected mod IDs to `compatible_mods.json`

### Bug Fixes
- Fixed incomplete state cleanup in shutdown() causing "can't rejoin" issues
  - v1.0.0: Only cleared activeHuds + previousUnderwaterState (2/5 Maps)
  - v1.0.2: Now clears all player state (waterEntryY, previousDepth, knownWorlds)

### User Experience
- **Before (v1.0.0)**: Get disconnect error → manually edit .ui file → rebuild plugin → redeploy
- **After (v1.0.2)**: Get disconnect error → exit to menu → run UPDATE.bat → restart server

---

## v1.0.0 (Initial Release)

### Core Features
- Real-time underwater depth tracking (local + sea level)
- Direction indicators (rising/descending)
- Automatic HUD show/hide at 0.5m threshold
- User commands: `/wdepth on`, `/wdepth off`, `/wdepth status`
- Basic MultipleHUD integration
