package com.underwaterdepth;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;

/**
 * Empty HUD used to clear/hide the depth gauge
 * Workaround for player.getHudManager().setCustomHud(null) causing crashes
 *
 * @author BeyondSmash
 */
public class EmptyHUD extends CustomUIHud {

    public EmptyHUD(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        // Intentionally empty - this HUD displays nothing
        // Used to replace active HUDs to effectively hide them
    }
}
