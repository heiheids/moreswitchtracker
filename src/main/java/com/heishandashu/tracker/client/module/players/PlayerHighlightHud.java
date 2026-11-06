package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Screen-edge arrows for highlighted players that are not on screen.
 *
 * <p>When the reticle cannot be seen -- the player is behind you, or off to the side -- an arrow
 * clamped to the screen border still points at them and carries the name and distance. That is
 * what makes a highlight usable anywhere in the dimension, not just in front of the camera.</p>
 */
public final class PlayerHighlightHud {

    private static final int HUD_COLOR = 0xFFFF7BD0;

    private PlayerHighlightHud() {
    }

    public static void register() {
        ClientEvents.addHudPass((graphics, partialTick) -> render(graphics));
    }

    private static void render(net.minecraft.client.gui.GuiGraphics graphics) {
        if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null || PlayerHighlightState.active().isEmpty()) {
            return;
        }
        if (minecraft.getWindow().getGuiScaledWidth() <= 0) {
            return;
        }

        String dimension = level.dimension().location().toString();
        for (PlayerHighlightState.Entry entry : PlayerHighlightState.active()) {
            Vec3 target = PlayerPositions.resolve(level, dimension, entry.uuid());
            if (target == null) {
                continue;
            }
            String label = entry.name()
                    + "  " + Component.translatable("tracker.players.distance",
                            String.format("%.0f", self.getEyePosition().distanceTo(target))).getString()
                    + "  " + PlayerHighlightRenderer.formatTime(entry.remaining());
            // Shared helper: draws the label when on screen, the edge arrow when not.
            PointHighlight.hud(graphics, target, label, HUD_COLOR);
        }
    }
}
