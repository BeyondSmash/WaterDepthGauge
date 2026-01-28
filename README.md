# WaterDepthGauge - Fix Compatibility Issues

## How to fix "Selected element in CustomUI command was not found. Selector: #MultipleHUD #ModName"

Need to have Java 21 or Java 25.

### Step 1: Get the Disconnect Error

1. Enter water with the new HUD mod installed
2. Note the mod ID from disconnect screen: `Selector: #MultipleHUD #ModName`
3. Exit to main menu (plugin auto-saves the mod ID to config)

### Step 2: Download Plugin Source

```bash
git clone https://github.com/BeyondSmash/WaterDepthGauge.git
cd WaterDepthGauge
```

Or download ZIP from: https://github.com/BeyondSmash/WaterDepthGauge

### Step 3: Run Auto-Update Script

**Windows:**
```bash
# Double-click UPDATE.bat
# Or run from command line:
UPDATE.bat
```

The script will:
- Read `compatible_mods.json` from `%AppData%\Hytale\UserData\Mods\WaterDepthGauge\`
- Find mods not yet in the `.ui` file
- Automatically add `Group #ModName` entries
- Build the plugin
- Deploy to your Mods folder

### Step 4: Verify and Restart

1. Check the output - should show: "Added 1 new mod(s) to .ui file"
2. Restart your server (full restart, not just reconnect)
3. Enter water - should work now!

---

See [ABOUT.md](ABOUT.md) for plugin information.
