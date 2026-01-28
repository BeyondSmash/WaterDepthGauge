package com.underwaterdepth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for compatible MultipleHUD mods
 * Tracks known compatible mods and their suggested container dimensions
 *
 * UNIVERSAL DEFAULT SYSTEM:
 * - The _DEFAULT entry provides default dimensions for auto-detected mods
 * - When a new mod is detected, it's auto-added using _DEFAULT dimensions
 * - Users can customize dimensions by editing individual entries or _DEFAULT
 *
 * LIMITATION:
 * - UI Groups are still hardcoded in the .ui file (cannot be generated dynamically)
 * - appendInline() crashes when loading files with variable declarations
 * - Users must rebuild the plugin after editing the source .ui file
 *
 * @author BeyondSmash
 */
public class CompatibleModsConfig {

    private static final Logger LOGGER = Logger.getLogger("UnderwaterDepth");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("compatibleHudMods")
    public List<CompatibleMod> compatibleHudMods = new ArrayList<>();

    /**
     * Represents a compatible HUD mod with its container dimensions
     */
    public static class CompatibleMod {
        @SerializedName("modId")
        public String modId;

        @SerializedName("width")
        public int width;

        @SerializedName("height")
        public int height;

        public CompatibleMod(String modId, int width, int height) {
            this.modId = modId;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Load config from file, creating default if doesn't exist
     */
    public static CompatibleModsConfig load(Path configPath) {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                CompatibleModsConfig config = GSON.fromJson(json, CompatibleModsConfig.class);
                LOGGER.info("[CompatibleModsConfig] Loaded config with " + config.compatibleHudMods.size() + " compatible mods");
                return config;
            } else {
                LOGGER.info("[CompatibleModsConfig] Config not found, creating default");
                CompatibleModsConfig config = createDefault();
                config.save(configPath);
                return config;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "[CompatibleModsConfig] Error loading config, using default: " + e.getMessage());
            return createDefault();
        }
    }

    /**
     * Save config to file
     */
    public void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            LOGGER.info("[CompatibleModsConfig] Saved config to " + configPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[CompatibleModsConfig] Error saving config: " + e.getMessage(), e);
        }
    }

    /**
     * Create default config with known compatible mods and universal default
     */
    private static CompatibleModsConfig createDefault() {
        CompatibleModsConfig config = new CompatibleModsConfig();

        // Universal default dimensions for auto-detected mods (generous sizing)
        config.compatibleHudMods.add(new CompatibleMod("_DEFAULT", 600, 500));

        // Add known compatible mods with their optimized container sizes
        config.compatibleHudMods.add(new CompatibleMod("BlockInfo", 300, 100));
        config.compatibleHudMods.add(new CompatibleMod("AdvancedItemInfo", 400, 300));
        config.compatibleHudMods.add(new CompatibleMod("AdminUI", 300, 200));

        return config;
    }

    /**
     * Check if a mod ID is already registered
     */
    public boolean hasModId(String modId) {
        return compatibleHudMods.stream()
                .anyMatch(mod -> mod.modId.equals(modId));
    }

    /**
     * Add a new mod using universal default dimensions
     */
    public void addMod(String modId) {
        if (!hasModId(modId)) {
            CompatibleMod defaultMod = getMod("_DEFAULT");
            int width = (defaultMod != null) ? defaultMod.width : 600;
            int height = (defaultMod != null) ? defaultMod.height : 500;

            compatibleHudMods.add(new CompatibleMod(modId, width, height));
            LOGGER.info("[CompatibleModsConfig] Added new mod: " + modId + " using default dimensions (" + width + "x" + height + ")");
        }
    }

    /**
     * Add a new mod with custom dimensions (override default)
     */
    public void addMod(String modId, int width, int height) {
        if (!hasModId(modId)) {
            compatibleHudMods.add(new CompatibleMod(modId, width, height));
            LOGGER.info("[CompatibleModsConfig] Added new mod: " + modId + " with custom dimensions (" + width + "x" + height + ")");
        }
    }

    /**
     * Get a mod by ID, or null if not found
     */
    public CompatibleMod getMod(String modId) {
        return compatibleHudMods.stream()
                .filter(mod -> mod.modId.equals(modId))
                .findFirst()
                .orElse(null);
    }
}
