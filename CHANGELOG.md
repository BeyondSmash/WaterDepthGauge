# Changelog

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
