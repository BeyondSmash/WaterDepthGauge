package com.underwaterdepth;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.buuz135.mhud.MultipleHUD;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * UnderwaterDepth Plugin - Depth meter for underwater exploration
 * Displays real-time depth in meters when player is submerged
 *
 * @author BeyondSmash
 * @version 1.0.0
 */
public class UnderwaterDepthPlugin extends JavaPlugin {

    private static final String PLUGIN_NAME = "UnderwaterDepth";
    private static final String VERSION = "1.0.4";

    // Configuration file name
    private static final String PLAYER_SETTINGS_FILE = "player_settings.json";

    // Gson for JSON serialization
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Hytale World Constants (based on community data)
    private static final int WORLD_BOTTOM = 0;      // Y=0 (bedrock layer around Y=3)
    private static final int WORLD_HEIGHT = 320;    // Y=320 (build limit)
    private static final int SEA_LEVEL = 115;       // Y=115 (water surface level)

    // Layer heights for reference (not used in depth calc)
    private static final int STONE_LEVEL = 111;     // Stone layer starts
    private static final int BASALT_LAYER_1 = 80;   // First basalt layer
    private static final int BACK_TO_STONE = 75;    // Returns to stone
    private static final int BASALT_LAYER_2 = 51;   // Second basalt layer
    private static final int VOLCANIC_LAYER = 38;   // Volcanic layer
    private static final int BEDROCK_LAYER = 3;     // Bedrock starts

    private static UnderwaterDepthPlugin instance;

    // MultipleHUD integration flag
    private boolean isMultipleHUDAvailable = false;

    // Track HUDs for each player
    private final Map<UUID, DepthHud> activeHuds = new HashMap<>();

    // Track previous underwater state to detect transitions
    private final Map<UUID, Boolean> previousUnderwaterState = new HashMap<>();

    // Track water entry Y coordinate for local depth calculation
    private final Map<UUID, Double> waterEntryY = new HashMap<>();

    // Track previous depth for directional arrows (rising/descending)
    private final Map<UUID, Float> previousDepth = new HashMap<>();

    // Track known worlds for player iteration
    private final Set<World> knownWorlds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Scheduled executor for depth updates
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> updateTask;

    public UnderwaterDepthPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static UnderwaterDepthPlugin getInstance() {
        return instance;
    }


    /**
     * Get the active HUD for a player
     */
    public DepthHud getActiveHud(UUID playerUuid) {
        return activeHuds.get(playerUuid);
    }

    /**
     * Set the active HUD for a player
     */
    public void setActiveHud(UUID playerUuid, DepthHud hud) {
        activeHuds.put(playerUuid, hud);
    }

    /**
     * Add a world to the known worlds set for player iteration
     */
    public void addKnownWorld(World world) {
        knownWorlds.add(world);
    }

    public void init() {
        getLogger().at(Level.INFO).log("=================================");
        getLogger().at(Level.INFO).log(PLUGIN_NAME + " v" + VERSION + " initializing...");
        getLogger().at(Level.INFO).log("=================================");
    }

    public void setup() {
        getLogger().at(Level.INFO).log("Setting up " + PLUGIN_NAME + "...");

        try {
            // Load player settings from disk
            loadPlayerSettings();

            // Register commands
            getCommandRegistry().registerCommand(new TestDepthCommand());
            getCommandRegistry().registerCommand(new WDepthCommand());
            getLogger().at(Level.INFO).log("Registered commands: /testdepth, /wdepth");

            // Register player join event to discover worlds
            // Use "default" world name as key - events are dispatched per world
            getEventRegistry().register(AddPlayerToWorldEvent.class, "default", this::onPlayerJoinWorld);
            getLogger().at(Level.INFO).log("Registered player join event");

            // Register player disconnect event to cleanup HUD state
            getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
            getLogger().at(Level.INFO).log("Registered player disconnect event");

            // Start scheduled depth update task
            startDepthUpdateTask();

            getLogger().at(Level.INFO).log(PLUGIN_NAME + " setup complete!");
            getLogger().at(Level.INFO).log("Depth meter HUD ready - automatic underwater detection active");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Setup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called when a player joins a world - track the world for automatic detection
     */
    private void onPlayerJoinWorld(AddPlayerToWorldEvent event) {
        World world = event.getWorld();
        knownWorlds.add(world);
        getLogger().at(Level.INFO).log("Player joined world, now tracking for underwater detection");
    }

    /**
     * Called when a player disconnects - cleanup all HUD state
     * CRITICAL: Prevents "can't rejoin" issue by clearing stuck HUD state
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();

        // Clean up ALL state for disconnecting player
        activeHuds.remove(uuid);
        waterEntryY.remove(uuid);
        previousUnderwaterState.remove(uuid);
        previousDepth.remove(uuid);

        getLogger().at(Level.INFO).log("[DISCONNECT] Cleaned up HUD state for player: " + uuid);
    }

    /**
     * Start the scheduled task that updates depth HUD for all players
     * Runs 4 times per second (250ms intervals) for smooth updates
     */
    private void startDepthUpdateTask() {
        getLogger().at(Level.INFO).log("Starting depth update task...");

        try {
            // Create single-threaded scheduler
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "UnderwaterDepth-UpdateTask");
                thread.setDaemon(true);
                return thread;
            });

            // Schedule depth updates at ~10 Hz (100ms intervals)
            updateTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Discover and check all worlds by iterating existing HUD players
                    // and also collecting worlds from those players
                    Set<World> worldsToCheck = new HashSet<>(knownWorlds);

                    // Add worlds from any active HUD players
                    for (DepthHud hud : new ArrayList<>(activeHuds.values())) {
                        try {
                            Ref<EntityStore> ref = hud.getPlayerRef().getReference();
                            if (ref != null && ref.isValid()) {
                                World world = ref.getStore().getExternalData().getWorld();
                                worldsToCheck.add(world);
                                knownWorlds.add(world);
                            }
                        } catch (Exception e) {
                            // Ignore invalid references
                        }
                    }

                    // Check all players in all discovered worlds
                    for (World world : worldsToCheck) {
                        // Must run on world thread for ECS access
                        world.execute(() -> {
                            try {
                                // Iterate all player references in this world
                                for (PlayerRef playerRefComp : world.getPlayerRefs()) {
                                    Ref<EntityStore> playerRef = playerRefComp.getReference();
                                    if (playerRef != null && playerRef.isValid()) {
                                        Store<EntityStore> store = playerRef.getStore();
                                        Player player = store.getComponent(playerRef, Player.getComponentType());
                                        if (player != null) {
                                            updatePlayerDepth(player);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                getLogger().at(Level.FINE).log("Error updating players in world: " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    getLogger().at(Level.WARNING).log("Error in update task: " + e.getMessage());
                }
            }, 100, 100, TimeUnit.MILLISECONDS); // Start after 100ms, repeat every 100ms

            getLogger().at(Level.INFO).log("Depth update task started (~10 updates/second)");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Failed to start update task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update depth HUD for a player
     * Called periodically to check if player is underwater and update display
     */
    public void updatePlayerDepth(Player player) {
        try {
            Ref<EntityStore> playerRef = player.getReference();
            Store<EntityStore> store = playerRef.getStore();

            // Get PlayerRef component for UUID
            PlayerRef playerRefComp = store.getComponent(
                playerRef,
                PlayerRef.getComponentType()
            );
            UUID playerUuid = playerRefComp.getUuid();

            // Check if HUD is enabled for this player
            PlayerConfig config = PlayerConfig.getConfig(playerUuid);
            if (!config.isEnabled()) {
                // HUD disabled - hide if showing
                if (activeHuds.containsKey(playerUuid)) {
                    hideDepthHud(player, playerUuid);
                }
                return;
            }

            // Get movement states to check if underwater
            MovementStatesComponent movementComp = store.getComponent(
                playerRef,
                MovementStatesComponent.getComponentType()
            );

            boolean isUnderwater = movementComp.getMovementStates().inFluid;
            boolean isJumping = movementComp.getMovementStates().jumping;
            Boolean wasUnderwater = previousUnderwaterState.get(playerUuid);

            // Player just entered water - record entry point but don't show HUD yet
            if (isUnderwater && (wasUnderwater == null || !wasUnderwater)) {
                // Record water entry Y coordinate
                TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
                if (transform != null) {
                    waterEntryY.put(playerUuid, transform.getPosition().getY());
                }
                getLogger().at(Level.FINE).log("Player " + playerUuid + " entered water - tracking depth");
            }
            // Player just surfaced - hide HUD and clear entry point
            else if (!isUnderwater && wasUnderwater != null && wasUnderwater) {
                hideDepthHud(player, playerUuid);
                waterEntryY.remove(playerUuid); // Clear entry point
                getLogger().at(Level.FINE).log("Player " + playerUuid + " surfaced - hiding depth HUD");
            }
            // Player is underwater - check depth and show/update HUD if needed
            else if (isUnderwater) {
                updateDepthWithThreshold(player, playerRefComp, store);
            }

            // Update state tracking
            previousUnderwaterState.put(playerUuid, isUnderwater);

        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Error updating player depth: " + e.getMessage());
        }
    }

    /**
     * Show the depth HUD for a player
     */
    private void showDepthHud(Player player, PlayerRef playerRefComp, float initialDepth, float initialSeaLevelDepth) {
        UUID uuid = playerRefComp.getUuid();

        // Create new HUD if doesn't exist
        if (!activeHuds.containsKey(uuid)) {
            try {
                getLogger().at(Level.INFO).log("[SHOW HUD] Player " + uuid + " - Creating new HUD with depth: " + initialDepth + "m, sea level: " + initialSeaLevelDepth + "m");

                DepthHud hud = new DepthHud(playerRefComp, initialDepth, initialSeaLevelDepth);

                // Show the HUD using MultipleHUD API for proper cross-mod compatibility
                // MultipleHUD handles showing internally, so we don't call hud.show() ourselves
                getLogger().at(Level.INFO).log("[SHOW HUD] Registering HUD with MultipleHUD");
                MultipleHUD.getInstance().setCustomHud(player, playerRefComp, DepthHud.ID, hud);

                // CRITICAL: Only add to activeHuds AFTER successful registration
                // This prevents "can't rejoin" issue if setCustomHud() throws exception
                activeHuds.put(uuid, hud);

                getLogger().at(Level.INFO).log("[SHOW HUD] HUD shown successfully and added to active HUDs");
            } catch (Exception e) {
                getLogger().at(Level.SEVERE).log("[SHOW HUD] FAILED to show HUD for player " + uuid + ": " + e.getMessage(), e);
                getLogger().at(Level.SEVERE).log("[SHOW HUD] Player will not see depth meter but won't be kicked. Error details:");
                e.printStackTrace();
                // Don't add to activeHuds if show failed - prevents crash loop on rejoin
            }
        }
    }

    /**
     * Hide the depth HUD for a player using MultipleHUD API
     */
    private void hideDepthHud(Player player, UUID uuid) {
        try {
            DepthHud hud = activeHuds.remove(uuid);

            if (hud != null) {
                getLogger().at(Level.INFO).log("[HIDE HUD] Player " + uuid + " - Hiding via MultipleHUD API");

                // Get player reference component
                Ref<EntityStore> playerRef = player.getReference();
                if (playerRef != null && playerRef.isValid()) {
                    Store<EntityStore> store = playerRef.getStore();
                    if (store != null) {
                        PlayerRef playerRefComp = store.getComponent(
                            playerRef,
                            PlayerRef.getComponentType()
                        );

                        if (playerRefComp != null) {
                            // Use MultipleHUD API to hide HUD
                            MultipleHUD.getInstance().hideCustomHud(player, playerRefComp, DepthHud.ID);
                            getLogger().at(Level.INFO).log("[HIDE HUD] HUD hidden via MultipleHUD");
                        }
                    }
                }
            } else {
                getLogger().at(Level.INFO).log("[HIDE HUD] Player " + uuid + " - No active HUD to hide");
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Error hiding HUD for player " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update depth with threshold check - only show HUD if depth >= 0.5m or <= -0.5m
     * This prevents HUD from showing during surface swimming
     */
    private void updateDepthWithThreshold(Player player, PlayerRef playerRefComp, Store<EntityStore> store) {
        UUID uuid = playerRefComp.getUuid();
        Ref<EntityStore> playerRef = player.getReference();

        // Calculate depth using real position
        float depth = calculateDepth(playerRef, store);

        // If very close to water entry point (within 0.3m), treat as surface (0m)
        // This prevents showing "-1m" or "+1m" when bobbing at the surface
        if (Math.abs(depth) < 0.3f) {
            depth = 0.0f;
        }

        // Check if depth meets threshold (≥0.5m below OR ≥0.5m above entry point)
        boolean shouldShowHud = Math.abs(depth) >= 0.5f;

        DepthHud hud = activeHuds.get(uuid);

        if (shouldShowHud) {
            // Calculate sea level depth using rounded-up water surface position
            // This gives clean whole numbers (e.g., Y=113.6 rounds to 114, shows as "1m below sea level")
            Double entryY = waterEntryY.get(uuid);
            float seaLevelDepth = 0.0f;
            if (entryY != null) {
                // Get current position for logging
                TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
                float currentY = transform != null ? (float) transform.getPosition().getY() : 0f;

                // Round up entry Y to next whole number (ceiling), then add 1.0 for surface compensation
                // Example: 113.6 -> ceil(113.6) = 114.0 -> 114.0 + 1.0 = 115.0
                float roundedSurface = (float) Math.ceil(entryY.doubleValue()) + 1.0f;
                // Calculate distance from sea level, then adjust by local depth
                seaLevelDepth = (SEA_LEVEL - roundedSurface) + depth;

                // Detailed logging for sea level calculation
                getLogger().at(Level.INFO).log(String.format(
                    "[SEA LEVEL] Entry Y: %.3f | Rounded Surface: %.1f (ceil+1) | Current Y: %.3f | Local Depth: %.1f | Sea Level Depth: %.1f",
                    entryY.doubleValue(), roundedSurface, currentY, depth, seaLevelDepth
                ));
            }

            // MISMATCH FIX: Hide HUD and recalibrate when sea level is 0m
            // This prevents showing misleading depths like -1.2m when at surface
            if (Math.round(seaLevelDepth) == 0) {
                // Recalibrate: Set local depth to 0m to match sea level
                depth = 0.0f;
                seaLevelDepth = 0.0f;

                // Hide HUD since we're at surface
                if (hud != null) {
                    hideDepthHud(player, uuid);
                }
                return;
            }

            // Calculate direction (rising/descending)
            Float prevDepth = previousDepth.get(uuid);
            int direction = 0; // 0 = stationary, 1 = descending, -1 = rising
            if (prevDepth != null) {
                if (depth > prevDepth + 0.1f) {
                    direction = 1; // Descending (going deeper)
                } else if (depth < prevDepth - 0.1f) {
                    direction = -1; // Rising (going shallower)
                }
            }
            previousDepth.put(uuid, depth);

            // Show HUD if not already shown
            if (hud == null) {
                showDepthHud(player, playerRefComp, depth, seaLevelDepth);
                hud = activeHuds.get(uuid);
            }

            // Update HUD with current depth, sea level, and direction
            if (hud != null) {
                hud.updateDepth(depth, seaLevelDepth, direction);
            }
        } else {
            // Hide HUD if showing but depth < 0.5m
            if (hud != null) {
                hideDepthHud(player, uuid);
            }
        }
    }

    /**
     * Update the depth value displayed in the HUD
     */
    private void updateDepth(Player player, PlayerRef playerRefComp) {
        UUID uuid = playerRefComp.getUuid();
        DepthHud hud = activeHuds.get(uuid);

        if (hud != null) {
            // Get player reference and store for component access
            Ref<EntityStore> playerRef = player.getReference();
            Store<EntityStore> store = playerRef.getStore();

            // Calculate depth using real position
            float depth = calculateDepth(playerRef, store);

            // Calculate sea level depth using rounded-up water surface position
            Double entryY = waterEntryY.get(uuid);
            float seaLevelDepth = 0.0f;
            if (entryY != null) {
                TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
                float currentY = transform != null ? (float) transform.getPosition().getY() : 0f;

                float roundedSurface = (float) Math.ceil(entryY.doubleValue()) + 1.0f;
                seaLevelDepth = (SEA_LEVEL - roundedSurface) + depth;

                getLogger().at(Level.INFO).log(String.format(
                    "[SEA LEVEL UPDATE] Entry Y: %.3f | Rounded Surface: %.1f (ceil+1) | Current Y: %.3f | Local Depth: %.1f | Sea Level Depth: %.1f",
                    entryY.doubleValue(), roundedSurface, currentY, depth, seaLevelDepth
                ));
            }

            // Update HUD with new depth and sea level (no direction tracking in updatePlayerDepthManual)
            hud.updateDepth(depth, seaLevelDepth, 0);
        }
    }

    /**
     * Calculate player's LOCAL depth relative to water entry point
     * Returns depth in meters (1 block = 1 meter)
     *
     * Positive values = below entry point (going deeper)
     * Negative values = above entry point (going shallower/surfacing)
     *
     * Formula: depth = entryY - currentY
     */
    private float calculateDepth(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        try {
            // Get player UUID
            PlayerRef playerRefComp = store.getComponent(playerRef, PlayerRef.getComponentType());
            UUID playerUuid = playerRefComp.getUuid();

            // Get water entry Y coordinate
            Double entryY = waterEntryY.get(playerUuid);
            if (entryY == null) {
                return 0.0f; // No entry point recorded
            }

            // Get TransformComponent for current player position
            TransformComponent transform = store.getComponent(
                playerRef,
                TransformComponent.getComponentType()
            );

            if (transform != null) {
                // Get precise Y position from transform
                double playerY = transform.getPosition().getY();

                // Calculate LOCAL depth relative to entry point
                // Positive = below entry (deeper), Negative = above entry (shallower)
                float localDepth = (float) (entryY - playerY);

                return localDepth;
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Failed to get player position: " + e.getMessage());
        }

        // Fallback: return 0 if position unavailable
        return 0.0f;
    }

    /**
     * Find the actual water surface Y coordinate above the player
     * This will be needed for accurate depth in areas with varying water levels
     *
     * @param player The player to check
     * @return Y coordinate of water surface (currently returns SEA_LEVEL constant)
     *
     * TODO: Implement block checking to find actual water/air boundary
     * - Ray-cast upward from player position
     * - Check each block until finding air
     * - Handle cases: lakes (< sea level), oceans (= sea level), underwater caves
     */
    private float findWaterSurface(Player player) {
        // TODO: When block API is available:
        // 1. Get player position
        // 2. Ray-cast upward checking blocks
        // 3. Find first air block (water surface)
        // 4. Return that Y coordinate

        // For now, assume standard sea level
        return SEA_LEVEL;
    }

    /**
     * Extract default UI file from JAR to external config directory
     * This allows users to edit the UI file without rebuilding the plugin
     */
    /**
     * Cleanup when plugin shuts down
     * CRITICAL: Clear all player state to prevent stuck HUD errors
     */
    public void shutdown() {
        getLogger().at(Level.INFO).log(PLUGIN_NAME + " shutting down...");

        // Stop scheduled tasks
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        // Clear ALL player state to prevent "can't rejoin" issues
        activeHuds.clear();
        previousUnderwaterState.clear();
        waterEntryY.clear();
        previousDepth.clear();
        knownWorlds.clear();

        getLogger().at(Level.INFO).log(PLUGIN_NAME + " shutdown complete - all player state cleared");
    }

    /**
     * Load player settings from disk
     */
    private void loadPlayerSettings() {
        Path settingsPath = getDataDirectory().resolve(PLAYER_SETTINGS_FILE);

        try {
            if (Files.exists(settingsPath)) {
                String json = Files.readString(settingsPath);
                Map<String, PlayerConfig> configMap = gson.fromJson(json, new TypeToken<Map<String, PlayerConfig>>(){}.getType());
                if (configMap != null) {
                    PlayerConfig.loadConfigs(configMap);
                    getLogger().at(Level.INFO).log("Loaded player settings from %s (%d players)", settingsPath, configMap.size());
                }
            } else {
                getLogger().at(Level.INFO).log("No existing player settings found - using defaults");
            }
        } catch (IOException e) {
            getLogger().at(Level.WARNING).log("Failed to load player settings: %s", e.getMessage());
        }
    }

    /**
     * Save player settings to disk
     */
    public void savePlayerSettings() {
        Path settingsPath = getDataDirectory().resolve(PLAYER_SETTINGS_FILE);

        try {
            // Ensure data directory exists
            Files.createDirectories(getDataDirectory());

            // Write player settings to JSON
            Map<String, PlayerConfig> configMap = PlayerConfig.getAllConfigs();
            String json = gson.toJson(configMap);
            Files.writeString(settingsPath, json);
            getLogger().at(Level.INFO).log("Saved player settings to %s (%d players)", settingsPath, configMap.size());
        } catch (IOException e) {
            getLogger().at(Level.WARNING).log("Failed to save player settings: %s", e.getMessage());
        }
    }
}
