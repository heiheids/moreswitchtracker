package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.CameraProjection;
import com.heishandashu.tracker.client.render.HudMarker;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.traders.WanderingTraderTrackingModule;
import com.heishandashu.tracker.net.TraderNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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
 * Draws the wandering trader: the despawn countdown above its head, and a marker when the player
 * highlighted it.
 *
 * <p>Everything is drawn on the HUD from the camera matrix rather than in the world. World-space
 * markers are placed from the player's tick position, which only changes twenty times a second, so
 * they stutter badly while the player walks; the HUD path the health bar uses does not.</p>
 *
 * <p>The overhead countdown shows for any tracked trader within
 * {@link WanderingTraderTrackingModule#OVERHEAD_RANGE} blocks, whether or not a highlight is
 * running -- that is the point of it: the trader vanishes on its own schedule, so the number has to
 * be readable while standing next to it.</p>
 */
public final class TraderOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "trader_arrows");

    private static final int COUNTDOWN_COLOR = 0xFFFFE9A8;
    private static final int MARK_COLOR = 0xFFFFC24A;
    private static final int DISTANCE_COLOR = 0xFFFFE6B0;
    private static final double ARROW_MARGIN = 18.0;
    private static final int ARROW_SIZE = 7;

    /** Marker height above the trader's feet, in blocks. */
    private static final double BODY_HEIGHT = 1.2;
    /** Half-size of the HUD marker square, in GUI pixels. */
    private static final int MARK_RADIUS = 7;
    /** Gap between the marker and the first label line, in GUI pixels. */
    private static final int LABEL_GAP = 5;

    private static boolean warned;

    private TraderOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(HUD_ID, TraderOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            return;
        }

        try {
            draw(graphics, minecraft, self, level);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                TrackerMod.LOGGER.warn("[tracker] trader overlay could not draw", t);
            }
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft,
                             LocalPlayer self, ClientLevel level) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        Vec3 eye = CameraProjection.eye(minecraft);
        String dimension = level.dimension().identifier().toString();
        float[] screen = new float[2];

        for (TraderNetworking.TraderSnap snap : TraderState.traders()) {
            if (!snap.dimension().equals(dimension)) {
                continue;
            }
            Vec3 where = TraderState.position(snap);
            double distance = eye.distanceTo(where);
            boolean highlighted = TraderState.isHighlighted(snap.uuid());
            Vec3 anchor = where.add(0.0, BODY_HEIGHT, 0.0);

            if (!CameraProjection.project(minecraft, anchor, width, height, screen)) {
                if (highlighted) {
                    drawEdgeArrow(graphics, minecraft, self, where, width, height, distance);
                }
                continue;
            }

            int centerX = Math.round(screen[0]);
            int centerY = Math.round(screen[1]);
            HudMarker.mark(graphics, centerX, centerY, MARK_RADIUS, MARK_COLOR);

            int lineY = centerY - MARK_RADIUS - LABEL_GAP - font.lineHeight;

            // A countdown of zero does not mean "about to vanish": it means this trader will never
            // despawn (spawn egg, /summon, or a saved trader).
            if (distance <= WanderingTraderTrackingModule.OVERHEAD_RANGE) {
                String countdown = snap.despawnTicks() > 0
                        ? Component.translatable("tracker.trader.countdown",
                                TraderState.clock(snap.despawnTicks())).getString()
                        : Component.translatable("tracker.trader.never_despawns").getString();
                HudMarker.centeredText(graphics, font, countdown, centerX, lineY,
                        width, height, COUNTDOWN_COLOR);
                lineY -= font.lineHeight + 1;
            }

            if (highlighted) {
                String caption = Component.translatable("tracker.players.distance",
                        String.format("%.0f", distance)).getString()
                        + "  " + TraderState.clock(TraderState.highlightRemaining(snap.uuid()));
                HudMarker.centeredText(graphics, font, caption, centerX, lineY,
                        width, height, DISTANCE_COLOR);
            }
        }
    }

    /** Edge arrow for a highlighted trader that is off screen or behind the player. */
    private static void drawEdgeArrow(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                      LocalPlayer self, Vec3 where, int width, int height,
                                      double distance) {
        ScreenProjection.Projected projected = ScreenProjection.project(
                CameraProjection.eye(minecraft), self.getViewVector(1.0f),
                minecraft.options.fov().get(), width, height, where, ARROW_MARGIN);
        if (projected.onScreen()) {
            return;
        }
        drawArrow(graphics, projected.screenX(), projected.screenY(),
                projected.dirX(), projected.dirY());
        String label = Component.translatable("tracker.players.distance",
                String.format("%.0f", distance)).getString();
        int textWidth = minecraft.font.width(label);
        int x = Math.max(2, Math.min(width - textWidth - 2,
                (int) Math.round(projected.screenX()) - textWidth / 2));
        int base = (int) Math.round(projected.screenY());
        int y = Math.max(2, Math.min(height - 10, base + (base > height / 2 ? -14 : 10)));
        graphics.text(minecraft.font, Component.literal(label), x, y, DISTANCE_COLOR);
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, double x, double y,
                                  double dirX, double dirY) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);
        if (Math.abs(dirX) >= Math.abs(dirY)) {
            int sign = dirX >= 0 ? 1 : -1;
            for (int i = 0; i < ARROW_SIZE; i++) {
                int edge = cx - sign * i;
                int half = i + 1;
                graphics.fill(Math.min(edge, edge + sign), cy - half,
                        Math.max(edge, edge + sign), cy + half, MARK_COLOR);
            }
        } else {
            int sign = dirY >= 0 ? -1 : 1;
            for (int i = 0; i < ARROW_SIZE; i++) {
                int edge = cy - sign * i;
                int half = i + 1;
                graphics.fill(cx - half, Math.min(edge, edge + sign),
                        cx + half, Math.max(edge, edge + sign), MARK_COLOR);
            }
        }
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.WANDERING_TRADER_TRACKING);
    }
}
