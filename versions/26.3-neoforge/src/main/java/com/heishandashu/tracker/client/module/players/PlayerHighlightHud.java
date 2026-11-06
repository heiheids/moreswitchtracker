package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Screen-edge arrows for highlighted players that are not on screen.
 *
 * <p>When the reticle cannot be seen -- the player is behind you, or off to the side -- an arrow
 * clamped to the screen border still points at them and carries the name and distance. That is
 * what makes a highlight usable anywhere in the dimension, not just in front of the camera.</p>
 */
public final class PlayerHighlightHud {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "player_highlight_arrows");

    private static final int ARROW_COLOR = 0xFFFF7BD0;
    private static final int TEXT_COLOR = 0xFFFFD6F2;
    /** How far inside the screen border the arrow sits. */
    private static final double MARGIN = 18.0;
    private static final int ARROW_SIZE = 7;

    private PlayerHighlightHud() {
    }

    public static void register() {
        TrackerClientEvents.onHudLayer(HUD_ID, PlayerHighlightHud::render);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null || PlayerHighlightState.active().isEmpty()) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Font font = minecraft.font;
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        String dimension = level.dimension().identifier().toString();

        for (PlayerHighlightState.Entry entry : PlayerHighlightState.active()) {
            Vec3 target = PlayerPositions.resolve(level, dimension, entry.uuid());
            if (target == null) {
                continue;
            }
            ScreenProjection.Projected projected = ScreenProjection.project(
                    eye, forward, fov, width, height, target, MARGIN);
            if (projected.onScreen()) {
                continue; // the in-world marker is visible, no arrow needed
            }
            drawArrow(graphics, projected.screenX(), projected.screenY(),
                    projected.dirX(), projected.dirY());
            drawLabel(graphics, font, width, height, projected, entry);
        }
    }

    /** A small filled triangle pointing the way the target is. */
    private static void drawArrow(GuiGraphicsExtractor graphics, double x, double y,
                                  double dirX, double dirY) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);

        if (Math.abs(dirX) >= Math.abs(dirY)) {
            int sign = dirX >= 0 ? 1 : -1; // pointing right or left
            for (int i = 0; i < ARROW_SIZE; i++) {
                int edge = cx - sign * i;
                int half = i + 1;
                int from = Math.min(edge, edge + sign);
                int to = Math.max(edge, edge + sign);
                graphics.fill(from, cy - half, to, cy + half, ARROW_COLOR);
            }
        } else {
            // Screen y grows downwards, so a target above the view points up.
            int sign = dirY >= 0 ? -1 : 1;
            for (int i = 0; i < ARROW_SIZE; i++) {
                int edge = cy - sign * i;
                int half = i + 1;
                int from = Math.min(edge, edge + sign);
                int to = Math.max(edge, edge + sign);
                graphics.fill(cx - half, from, cx + half, to, ARROW_COLOR);
            }
        }
    }

    private static void drawLabel(GuiGraphicsExtractor graphics, Font font, int width, int height,
                                  ScreenProjection.Projected projected,
                                  PlayerHighlightState.Entry entry) {
        String label = entry.name()
                + "  " + Component.translatable("tracker.players.distance",
                        String.format("%.0f", projected.distance())).getString()
                + "  " + PlayerHighlightRenderer.formatTime(entry.remaining());

        int textWidth = font.width(label);
        int x = clamp((int) Math.round(projected.screenX()) - textWidth / 2, 2, Math.max(2, width - textWidth - 2));
        int base = (int) Math.round(projected.screenY());
        int y = clamp(base + (base > height / 2 ? -14 : 10), 2, Math.max(2, height - 10));
        graphics.text(font, Component.literal(label), x, y, TEXT_COLOR);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return value < min ? min : Math.min(value, max);
    }
}
