package com.underwaterdepth;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.concurrent.CompletableFuture;

/**
 * Main command for underwater depth HUD configuration
 * Usage:
 *   /wdepth on - Enable HUD
 *   /wdepth off - Disable HUD
 *   /wdepth sea on - Show sea level display
 *   /wdepth sea off - Hide sea level display
 *   /wdepth decimal on - Enable decimal display
 *   /wdepth decimal off - Disable decimal display
 *
 * @author BeyondSmash
 */
public class WDepthCommand extends AbstractCommand {

    public WDepthCommand() {
        super("wdepth", "Configure underwater depth HUD (usage: /wdepth <on | off | sea | decimal | credits> [value])");
        setPermissionGroup(GameMode.Adventure);
        setAllowsExtraArguments(true);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("This command can only be used by players!").color("#ff5555"));
            return CompletableFuture.completedFuture(null);
        }

        var sender = context.sender();
        if (!(sender instanceof Player)) {
            return CompletableFuture.completedFuture(null);
        }

        Player player = (Player) sender;
        var world = player.getWorld();

        if (world == null) {
            context.sendMessage(Message.raw("Unable to execute command - player not in world").color("#ff5555"));
            return CompletableFuture.completedFuture(null);
        }

        // Execute on world thread to access ECS components safely
        CompletableFuture<Void> future = new CompletableFuture<>();
        world.execute(() -> {
            try {
                var playerRef = player.getReference();
                var store = playerRef.getStore();
                var playerRefComp = store.getComponent(playerRef, com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                var uuid = playerRefComp.getUuid();
                PlayerConfig config = PlayerConfig.getConfig(uuid);

                // Get command arguments
                String input = context.getInputString().trim();
                String[] args = input.split("\\s+");

                // Remove command name from args
                if (args.length > 0 && args[0].equalsIgnoreCase("wdepth")) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    args = newArgs;
                }

                // No arguments - show current config
                if (args.length == 0) {
                    showConfig(context, config);
                    future.complete(null);
                    return;
                }

                String subcommand = args[0].toLowerCase();

                switch (subcommand) {
                    case "on":
                        config.setEnabled(true);
                        context.sendMessage(Message.raw("Underwater depth HUD enabled").color("#55ff55"));
                        break;

                    case "off":
                        config.setEnabled(false);
                        context.sendMessage(Message.raw("Underwater depth HUD disabled").color("#ff5555"));
                        break;

                    case "credits":
                        context.sendMessage(Message.raw("Water Depth Gauge plugin created by BeyondSmash").color("#ffaa00"));
                        break;

                    case "sea":
                        if (args.length < 2) {
                            context.sendMessage(Message.raw("Usage: /wdepth sea <on | off>").color("#ff5555"));
                            future.complete(null);
                            return;
                        }
                        if (args[1].equalsIgnoreCase("on")) {
                            config.setSeaLevelDisplayEnabled(true);
                            context.sendMessage(Message.raw("Sea level display enabled").color("#55ff55"));
                        } else if (args[1].equalsIgnoreCase("off")) {
                            config.setSeaLevelDisplayEnabled(false);
                            context.sendMessage(Message.raw("Sea level display disabled").color("#ff5555"));
                        } else {
                            context.sendMessage(Message.raw("Usage: /wdepth sea <on | off>").color("#ff5555"));
                        }
                        break;

                    case "decimal":
                        if (args.length < 2) {
                            context.sendMessage(Message.raw("Usage: /wdepth decimal <on | off>").color("#ff5555"));
                            future.complete(null);
                            return;
                        }
                        if (args[1].equalsIgnoreCase("on")) {
                            config.setDecimalEnabled(true);
                            context.sendMessage(Message.raw("Decimal display enabled (e.g., 3.1m)").color("#55ff55"));
                        } else if (args[1].equalsIgnoreCase("off")) {
                            config.setDecimalEnabled(false);
                            context.sendMessage(Message.raw("Decimal display disabled (whole numbers only)").color("#ff5555"));
                        } else {
                            context.sendMessage(Message.raw("Usage: /wdepth decimal <on | off>").color("#ff5555"));
                        }
                        break;

                    default:
                        context.sendMessage(Message.raw("Unknown subcommand: " + subcommand).color("#ff5555"));
                        context.sendMessage(Message.raw("Usage: /wdepth <on | off | sea | decimal | credits> [value]").color("#aaaaaa"));
                        break;
                }

                future.complete(null);
            } catch (Exception e) {
                context.sendMessage(Message.raw("Error executing command: " + e.getMessage()).color("#ff5555"));
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    private void showConfig(CommandContext context, PlayerConfig config) {
        // Title
        context.sendMessage(
            Message.raw("=== Underwater Depth HUD Settings ===")
                .color("#ffaa00")
                .bold(true)
        );

        // HUD Enabled
        context.sendMessage(
            Message.raw("HUD Enabled: ")
                .color("#aaaaaa")
                .insert(Message.raw(config.isEnabled() ? "ON" : "OFF")
                    .color(config.isEnabled() ? "#55ff55" : "#ff5555"))
        );

        // Sea Level Display
        context.sendMessage(
            Message.raw("Sea Level Display: ")
                .color("#aaaaaa")
                .insert(Message.raw(config.isSeaLevelDisplayEnabled() ? "ON" : "OFF")
                    .color(config.isSeaLevelDisplayEnabled() ? "#55ff55" : "#ff5555"))
        );

        // Decimal Mode
        context.sendMessage(
            Message.raw("Decimal Mode: ")
                .color("#aaaaaa")
                .insert(Message.raw(config.isDecimalEnabled() ? "ON" : "OFF")
                    .color(config.isDecimalEnabled() ? "#55ff55" : "#ff5555"))
        );

        // Commands
        context.sendMessage(
            Message.raw("Commands: ")
                .color("#aaaaaa")
                .insert(Message.raw("/wdepth <on | off | sea | decimal | credits>")
                    .color("#ffffff"))
        );
    }
}
