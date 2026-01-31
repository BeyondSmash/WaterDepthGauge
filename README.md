# WaterDepthGauge

A Hytale plugin that displays a real-time depth gauge when underwater, showing both local water depth and depth relative to sea level.

## Features

- **Real-time depth tracking** - Shows depth in meters below the water surface
- **Sea level reference** - Optional display of depth relative to Y=115 (sea level)
- **Direction indicators** - Up/down arrows show if you're ascending or descending
- **Dynamic gauge** - Automatically adjusts range as you dive deeper (0-20m, 20-40m, etc.)
- **Smooth animations** - Gauge updates in real-time with 500ms refresh rate
- **Configurable display** - Toggle between whole numbers and decimal precision
- **Cross-mod compatible** - Works with ALL MultipleHUD-compatible mods (Blocchio, BlockInfo, EyeSpyHUD, etc.)

## Requirements

- **Java:** 21 or 25
- **Hytale:** Early Access (January 2026 or later)
- **Dependencies:** [MultipleHUD](https://www.curseforge.com/hytale/hytale-mods/multiplehud) v1.0.1 or higher

## Installation

1. Download the latest release: [WaterDepthGauge-1.0.3.jar](https://github.com/BeyondSmash/WaterDepthGauge/releases/latest)
2. Place the JAR in: `%AppData%\Roaming\Hytale\UserData\Mods\`
3. Ensure MultipleHUD is also installed
4. Start your server and join the game

## Usage

The depth gauge automatically appears when you submerge underwater (≥0.5m depth) and disappears when you surface.

### Commands

- `/wdepth status` - Check if the gauge is enabled
- `/wdepth on` - Enable the depth gauge
- `/wdepth off` - Disable the depth gauge

### Configuration

Player-specific settings are stored in:
`%AppData%\Roaming\Hytale\UserData\Saves\[WORLD_NAME]\mods\BeyondSmash_WaterDepthGauge\config.json`

**Available settings:**
```json
{
  "decimalEnabled": true,          // Show decimal precision (e.g., "5.3m" vs "5m")
  "seaLevelDisplayEnabled": true   // Show "Units below sea level: 5m" text
}
```

## How It Works

- **Local Depth**: Measured from water entry point (resets each time you enter water)
- **Sea Level Depth**: Measured from Y=115 (Hytale's sea level)
- **Gauge Range**: Dynamically adjusts in 20m increments (0-20m, 20-40m, 40-60m, etc.)
- **Color Coding**: Markers turn pale orange at 5m intervals (5m, 10m, 15m, etc.)
- **Direction Arrow**: Yellow arrow shows ascending (^) or descending (v) movement

## v1.0.3 Update - Proper MultipleHUD Integration

This version completely refactored the plugin to use proper MultipleHUD API integration:

- **Removed 500+ lines of legacy compatibility code**
- **No more manual configuration needed** - Just install and play
- **Works with ALL MultipleHUD-compatible mods** - Tested with Blocchio, BlockInfo, EyeSpyHUD
- **Fixed cross-mod conflicts** - No longer uses "dominant UI system" that overrode MultipleHUD's container

See [CHANGELOG.md](CHANGELOG.md) for full details.

## Building from Source

```bash
git clone https://github.com/BeyondSmash/WaterDepthGauge.git
cd WaterDepthGauge
./gradlew shadowJar
```

Output: `build/libs/WaterDepthGauge-1.0.3.jar`

## Support

- **Issues:** https://github.com/BeyondSmash/WaterDepthGauge/issues
- **Releases:** https://github.com/BeyondSmash/WaterDepthGauge/releases

## License

This project is open source. See the repository for license details.

## Credits

Created by BeyondSmash
Built with [MultipleHUD](https://www.curseforge.com/hytale/hytale-mods/multiplehud) by Buuz135
