package com.underwaterdepth;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.concurrent.CompletableFuture;

/**
 * Test command to manually show/hide the depth HUD for testing
 * Usage: /testdepth [depth]
 */
public class TestDepthCommand extends AbstractCommand {

    private final DefaultArg<Float> depthArg = this.withDefaultArg(
        "depth", "Depth in meters (0-115)", ArgTypes.FLOAT, 10.0f, "10.0"
    );

    public TestDepthCommand() {
        super("testdepth", "Test the underwater depth HUD (usage: /testdepth [depth in meters])");
        setPermissionGroup(GameMode.Adventure);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!"));
            return CompletableFuture.completedFuture(null);
        }

        var sender = context.sender();
        if (!(sender instanceof Player)) {
            return CompletableFuture.completedFuture(null);
        }

        Player player = (Player) sender;

        // Get depth argument (has default value of 10.0)
        float depth = context.get(depthArg);

        // Clamp depth to valid range
        if (depth < 0) depth = 0;
        if (depth > 115) depth = 115; // Max depth to bedrock

        final float finalDepth = depth;

        // Get world from player (thread-safe method)
        var world = player.getWorld();
        if (world == null) {
            context.sendMessage(Message.raw("Unable to test depth HUD - player not in world"));
            return CompletableFuture.completedFuture(null);
        }

        // Add world to plugin's known worlds for automatic detection
        UnderwaterDepthPlugin.getInstance().addKnownWorld(world);

        // Run on world thread for ECS access
        CompletableFuture<Void> future = new CompletableFuture<>();
        world.execute(() -> {
            try {
                var playerRef = player.getReference();
                if (playerRef == null || !playerRef.isValid()) {
                    context.sendMessage(Message.raw("Unable to test depth HUD - player not in world"));
                    future.complete(null);
                    return;
                }

                var store = playerRef.getStore();
                var playerRefComponent = store.getComponent(playerRef, PlayerRef.getComponentType());

                if (playerRefComponent == null) {
                    context.sendMessage(Message.raw("Unable to test depth HUD - player reference not found"));
                    future.complete(null);
                    return;
                }

                var uuid = playerRefComponent.getUuid();
                UnderwaterDepthPlugin plugin = UnderwaterDepthPlugin.getInstance();

                // Get or create HUD with initial depth
                DepthHud hud = plugin.getActiveHud(uuid);
                if (hud == null) {
                    hud = new DepthHud(playerRefComponent, finalDepth, 0.0f);
                    plugin.setActiveHud(uuid, hud);
                    hud.show();
                } else {
                    hud.updateDepth(finalDepth, 0.0f, 0);
                }

                context.sendMessage(Message.raw("Depth HUD shown with depth: " + Math.round(finalDepth) + "m").color("#55ff55"));
                context.sendMessage(Message.raw("Use /testdepth --depth=0 to test surface").color("#aaaaaa"));

                future.complete(null);
            } catch (Exception e) {
                context.sendMessage(Message.raw("Error showing HUD: " + e.getMessage()).color("#ff5555"));
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
