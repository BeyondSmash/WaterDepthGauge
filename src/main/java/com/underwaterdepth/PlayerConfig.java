package com.underwaterdepth;

import java.util.HashMap;
import java.util.Map;
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

    public PlayerConfig() {
        // Public constructor for Gson deserialization
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

    /**
     * Load all player configs from a map (used for deserialization)
     */
    public static void loadConfigs(Map<String, PlayerConfig> configMap) {
        configs.clear();
        for (Map.Entry<String, PlayerConfig> entry : configMap.entrySet()) {
            try {
                UUID uuid = UUID.fromString(entry.getKey());
                configs.put(uuid, entry.getValue());
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }
    }

    /**
     * Get all player configs as a map (used for serialization)
     */
    public static Map<String, PlayerConfig> getAllConfigs() {
        Map<String, PlayerConfig> configMap = new HashMap<>();
        for (Map.Entry<UUID, PlayerConfig> entry : configs.entrySet()) {
            configMap.put(entry.getKey().toString(), entry.getValue());
        }
        return configMap;
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
