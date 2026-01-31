package com.underwaterdepth;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomHud;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Custom HUD that displays underwater depth in meters with gauge image
 * Integrates with MultipleHUD library via standard registration API
 *
 * @author BeyondSmash
 */
public class DepthHud extends CustomUIHud {

    private static final Logger LOGGER = Logger.getLogger("UnderwaterDepth");
    public static final String ID = "WaterDepthGauge";

    private float currentDepth = 0f;
    private float currentSeaLevelDepth = 0f;

    // Dynamic gauge ranges
    private static final int BASE_RANGE = 20;  // 0-20m base range

    public DepthHud(PlayerRef playerRef, float initialDepth, float initialSeaLevelDepth) {
        super(playerRef);
        this.currentDepth = initialDepth;
        this.currentSeaLevelDepth = initialSeaLevelDepth;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        LOGGER.info("[DepthHud] Building HUD from embedded UI file");
        LOGGER.info("[DepthHud] Initial depth: " + currentDepth + "m, sea level: " + currentSeaLevelDepth + "m");

        try {
            // Load embedded UI file from JAR resources
            String uiPath = "Hud/UnderwaterDepth/UnderwaterDepth_DepthMeter.ui";
            LOGGER.info("[DepthHud] Loading UI file: " + uiPath);
            builder.append(uiPath);

            // Set initial gauge state
            LOGGER.info("[DepthHud] Setting initial gauge state");
            updateGauge(builder, currentDepth, currentSeaLevelDepth, 0, true);

            LOGGER.info("[DepthHud] Build complete");
        } catch (Exception e) {
            LOGGER.severe("[DepthHud] Error building HUD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the displayed depth value
     *
     * @param depth Depth in meters (relative to water entry)
     * @param seaLevelDepth Depth relative to sea level (Y=115)
     * @param direction Direction of movement: 1 = descending, -1 = rising, 0 = stationary
     */
    public void updateDepth(float depth, float seaLevelDepth, int direction) {
        this.currentDepth = depth;
        this.currentSeaLevelDepth = seaLevelDepth;

        // Log update frequency (every 50th update to avoid spam)
        updateCount++;
        if (updateCount % 50 == 0) {
            LOGGER.fine("[DepthHud] Update #" + updateCount + " - depth=" + depth + "m, sea=" + seaLevelDepth + "m, dir=" + direction);
        }

        // Create update builder
        UICommandBuilder builder = new UICommandBuilder();

        // Update the gauge
        updateGauge(builder, depth, seaLevelDepth, direction, false);

        // Apply update
        update(false, builder);
    }

    private int updateCount = 0;

    /**
     * Update gauge based on depth - dynamic range with labeled ticks
     */
    private void updateGauge(UICommandBuilder builder, float depth, float seaLevelDepth, int direction, boolean isInitialBuild) {
        LOGGER.info("[DepthHud] updateGauge called - depth=" + depth + ", seaLevel=" + seaLevelDepth + ", direction=" + direction + ", isInitialBuild=" + isInitialBuild);

        // Get player config
        PlayerRef playerRef = getPlayerRef();
        java.util.UUID uuid = playerRef.getUuid();
        PlayerConfig config = PlayerConfig.getConfig(uuid);

        // Get absolute depth
        float absDepth = Math.abs(depth);
        boolean isAboveSurface = depth < 0;

        // Determine range and tick labels based on depth
        int baseRange = (int)(Math.floor(absDepth / BASE_RANGE) * BASE_RANGE);
        boolean isNegativeRange = isAboveSurface && absDepth >= 1.0f;

        // Update sea level display (or hide if disabled)
        updateSeaLevelDisplay(builder, seaLevelDepth, config.isSeaLevelDisplayEnabled());

        // Update local depth label with directional arrow
        updateLocalDepthLabel(builder, direction);

        // Show gauge and update tick labels for current range
        showGaugeElements(builder, baseRange, isNegativeRange);

        // Calculate marker position within current 20m range
        float depthInRange = absDepth - baseRange;
        float normalizedDepth = Math.min(depthInRange / BASE_RANGE, 1.0f);

        // Map to marker indices (0-59) - now 60 positions instead of 15
        int markerIndex = Math.round(normalizedDepth * 59);
        markerIndex = Math.max(0, Math.min(59, markerIndex));

        // For negative ranges (above surface), invert marker position
        if (isNegativeRange) {
            markerIndex = 59 - markerIndex;
        }

        // Format depth text with decimal or whole number based on config
        String prefix = isAboveSurface ? "+" : "-";
        String depthText;
        if (config.isDecimalEnabled()) {
            depthText = prefix + String.format("%.1f", absDepth) + "m";
        } else {
            depthText = prefix + Math.round(absDepth) + "m";
        }

        // Determine marker color based on depth
        String markerColor = getMarkerColor(absDepth, config.isDecimalEnabled());

        LOGGER.info("[DepthHud] Depth: " + depthText + " - range: " + baseRange + "-" + (baseRange + BASE_RANGE) + " - marker: " + markerIndex + " - color: " + markerColor);

        // Show only the active marker, hide all others
        updateMarkerVisibility(builder, markerIndex, depthText, markerColor);
    }

    /**
     * Get marker color based on depth value
     * - #FFE6B5 (pale orange) for 5m intervals (5.0, 10.0, 15.0, etc.)
     * - #ffffff (white) for all other depths
     */
    private String getMarkerColor(float absDepth, boolean decimalEnabled) {
        // If decimal mode is off, check whole number for 5m intervals
        if (!decimalEnabled) {
            int wholeDepth = Math.round(absDepth);
            if (wholeDepth % 5 == 0 && wholeDepth > 0) {
                return "#FFE6B5"; // 5m intervals (pale orange)
            }
            return "#ffffff"; // All other depths (white)
        }

        // Check if it's a 5m interval (within 0.05m tolerance)
        float remainder5 = absDepth % 5.0f;
        if (remainder5 < 0.05f || remainder5 > 4.95f) {
            return "#FFE6B5"; // 5m intervals (pale orange)
        }

        // All other depths (decimals and whole numbers)
        return "#ffffff"; // White
    }

    /**
     * Update local depth label with directional indicator
     */
    private void updateLocalDepthLabel(UICommandBuilder builder, int direction) {
        // Update label text
        builder.set("#LocalDepthLabel.Text", "Local Water Depth:");

        // Update directional arrow using simple text characters
        if (direction > 0) {
            // Descending - show down arrow
            builder.set("#DirectionIcon.Text", "v");
        } else if (direction < 0) {
            // Rising - show up arrow
            builder.set("#DirectionIcon.Text", "^");
        } else {
            // Stationary - hide by clearing text
            builder.set("#DirectionIcon.Text", "");
        }
    }

    /**
     * Update sea level display text (or hide if disabled)
     */
    private void updateSeaLevelDisplay(UICommandBuilder builder, float seaLevelDepth, boolean enabled) {
        if (!enabled) {
            // Hide sea level display
            builder.set("#SeaLevelText.Text", "");
            builder.set("#SeaLevelValue.Text", "");
            return;
        }

        String direction = seaLevelDepth > 0 ? "below" : "above";
        float absSeaLevel = Math.abs(seaLevelDepth);

        // Format combined text: "Units below sea level: 5m"
        // The value is bold/larger via separate label with fixed positioning
        builder.set("#SeaLevelText.Text", "Units " + direction + " sea level:");
        builder.set("#SeaLevelValue.Text", Math.round(absSeaLevel) + "m");
    }

    /**
     * Show gauge tick labels based on current range
     * For negative range (above surface), labels are reversed bottom-to-top
     */
    private void showGaugeElements(UICommandBuilder builder, int baseRange, boolean isNegativeRange) {
        if (isNegativeRange) {
            // Above surface: reversed order (18 at bottom, 3 at top)
            builder.set("#Tick3m.Text", (baseRange + 18) + "m");
            builder.set("#Tick6m.Text", (baseRange + 15) + "m");
            builder.set("#Tick9m.Text", (baseRange + 12) + "m");
            builder.set("#Tick12m.Text", (baseRange + 9) + "m");
            builder.set("#Tick15m.Text", (baseRange + 6) + "m");
            builder.set("#Tick18m.Text", (baseRange + 3) + "m");
        } else {
            // Below surface: normal order
            builder.set("#Tick3m.Text", (baseRange + 3) + "m");
            builder.set("#Tick6m.Text", (baseRange + 6) + "m");
            builder.set("#Tick9m.Text", (baseRange + 9) + "m");
            builder.set("#Tick12m.Text", (baseRange + 12) + "m");
            builder.set("#Tick15m.Text", (baseRange + 15) + "m");
            builder.set("#Tick18m.Text", (baseRange + 18) + "m");
        }
    }

    /**
     * Update marker visibility - show only the active marker at the specified position
     */
    private void updateMarkerVisibility(UICommandBuilder builder, int activeIndex, String depthText, String color) {
        for (int i = 0; i <= 59; i++) {
            if (i == activeIndex) {
                builder.set("#Marker" + i + ".Text", depthText);
                builder.set("#Marker" + i + ".Style.TextColor", color);
            } else {
                builder.set("#Marker" + i + ".Text", "");
            }
        }
    }

    /**
     * Clear/hide the HUD by sending a clear packet directly to the client
     * This completely removes the HUD including background texture
     */
    public void clearHud() {
        try {
            PlayerRef playerRef = getPlayerRef();
            if (playerRef != null && playerRef.getPacketHandler() != null) {
                LOGGER.info("[DepthHud] Sending clear packet to client");
                // Send clear packet with empty commands array (not null - client crashes on null)
                playerRef.getPacketHandler().writeNoCache(new CustomHud(true, new CustomUICommand[0]));
            } else {
                LOGGER.warning("[DepthHud] Cannot clear HUD - PlayerRef or PacketHandler is null");
            }
        } catch (Exception e) {
            LOGGER.warning("[DepthHud] Error clearing HUD: " + e.getMessage());
        }
    }

}
