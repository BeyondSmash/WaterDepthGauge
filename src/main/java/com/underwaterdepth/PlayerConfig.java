package com.underwaterdepth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player configuration for underwater depth HUD
 *
 * @author BeyondSmash
 */
public class PlayerConfig {

    private static final ConcurrentHashMap<UUID, PlayerConfig> configs = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private boolean seaLevelDisplayEnabled = true;
    private int opacity = 100; // 0-100%
    private boolean decimalEnabled = true;

    private PlayerConfig() {
    }

    /**
     * Get or create config for a player
     */
    public static PlayerConfig getConfig(UUID uuid) {
        return configs.computeIfAbsent(uuid, k -> new PlayerConfig());
    }

    /**
     * Remove config for a player (cleanup on logout)
     */
    public static void removeConfig(UUID uuid) {
        configs.remove(uuid);
    }

    // Getters and Setters

    public boolean isEnabled() {
        return enabled && opacity > 0;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSeaLevelDisplayEnabled() {
        return seaLevelDisplayEnabled;
    }

    public void setSeaLevelDisplayEnabled(boolean enabled) {
        this.seaLevelDisplayEnabled = enabled;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = Math.max(0, Math.min(100, opacity));
    }

    public boolean isDecimalEnabled() {
        return decimalEnabled;
    }

    public void setDecimalEnabled(boolean enabled) {
        this.decimalEnabled = enabled;
    }
}
