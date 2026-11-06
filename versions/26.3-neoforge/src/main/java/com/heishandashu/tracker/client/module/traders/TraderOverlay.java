package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.traders.WanderingTraderTrackingModule;
import com.heishandashu.tracker.net.TraderNetworking;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the wandering trader: the despawn countdown above its head, and a reticle when the player
 * highlighted it.
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
    private static final double SCREEN_FACTOR = 0.035;
    private static final double NEAR_LIMIT = 24.0;
    private static final double ANCHOR_DISTANCE = 12.0;
    private static final double ARROW_MARGIN = 18.0;
    private static final int ARROW_SIZE = 7;

    private static boolean warned;

    private TraderOverlay() {
    }

    public static void register() {
        TrackerClientEvents.onGizmos(() -> {
            if (!enabled()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer self = minecraft.player;
            if (level == null || self == null) {
                return;
            }
            try {
                String dimension = level.dimension().identifier().toString();
                Vec3 eye = self.getEyePosition();
                Vec3 view = self.getViewVector(1.0f);

                for (TraderNetworking.TraderSnap snap : TraderState.traders()) {
                    if (!snap.dimension().equals(dimension)) {
                        continue;
                    }
                    double distance = TraderState.distance(snap);
                    Vec3 where = TraderState.position(snap);
                    boolean highlighted = TraderState.isHighlighted(snap.uuid());

                    // One anchor and one line spacing for both labels, so the despawn countdown and
                    // the highlight countdown stack instead of landing on the same spot.
                    // Close by they use fixed world offsets above the head; far away the marker is
                    // anchored along the view direction and the offsets scale with it, which keeps
                    // the on-screen spacing constant too.
                    Vec3 drawAt;
                    double lineOffset;
                    double lineStep;
                    if (distance <= NEAR_LIMIT) {
                        drawAt = where;
                        lineOffset = 1.0;
                        lineStep = 0.55;
                    } else {
                        Vec3 direction = where.subtract(eye);
                        double length = direction.length();
                        if (length < 0.05) {
                            continue;
                        }
                        direction = direction.scale(1.0 / length);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind the player: nothing sensible to draw
                        }
                        drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                        double anchorRadius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                        lineOffset = anchorRadius * 1.9;
                        lineStep = anchorRadius;
                    }

                    // 1) the despawn countdown, for any tracked trader the player is close to --
                    //    whether or not a highlight is running
                    if (distance <= WanderingTraderTrackingModule.OVERHEAD_RANGE) {
                        // A countdown of zero does not mean "about to vanish": it means this
                        // trader will never despawn (spawn egg, /summon, or a saved trader).
                        String countdown = snap.despawnTicks() > 0
                                ? Component.translatable("tracker.trader.countdown",
                                        TraderState.clock(snap.despawnTicks())).getString()
                                : Component.translatable("tracker.trader.never_despawns").getString();
                        Gizmos.billboardText(countdown, drawAt.add(0.0, lineOffset, 0.0),
                                        TextGizmo.Style.forColorAndCentered(COUNTDOWN_COLOR).withScale(0.8f))
                                .setAlwaysOnTop();
                        lineOffset += lineStep;
                    }

                    // 2) only when highlighted: the reticle plus its own countdown, one line above
                    if (!highlighted) {
                        continue;
                    }
                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                    TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);

                    String caption = Component.translatable("tracker.players.distance",
                            String.format("%.0f", distance)).getString()
                            + "  " + TraderState.clock(TraderState.highlightRemaining(snap.uuid()));
                    Gizmos.billboardText(caption, drawAt.add(0.0, lineOffset, 0.0),
                                    TextGizmo.Style.forColorAndCentered(DISTANCE_COLOR).withScale(0.85f))
                            .setAlwaysOnTop();
                }
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] trader overlay could not draw", t);
                }
            }
        });

        TrackerClientEvents.onHudLayer(HUD_ID, TraderOverlay::renderArrows);
    }

    /** Edge arrows for a highlighted trader that is off screen or behind the player. */
    private static void renderArrows(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        String dimension = level.dimension().identifier().toString();

        for (TraderNetworking.TraderSnap snap : TraderState.traders()) {
            if (!snap.dimension().equals(dimension) || !TraderState.isHighlighted(snap.uuid())) {
                continue;
            }
            ScreenProjection.Projected projected = ScreenProjection.project(
                    eye, forward, fov, width, height, TraderState.position(snap), ARROW_MARGIN);
            if (projected.onScreen()) {
                continue;
            }
            drawArrow(graphics, projected.screenX(), projected.screenY(), projected.dirX(), projected.dirY());
            String label = Component.translatable("tracker.players.distance",
                    String.format("%.0f", projected.distance())).getString();
            int textWidth = font.width(label);
            int x = Math.max(2, Math.min(width - textWidth - 2,
                    (int) Math.round(projected.screenX()) - textWidth / 2));
            int base = (int) Math.round(projected.screenY());
            int y = Math.max(2, Math.min(height - 10, base + (base > height / 2 ? -14 : 10)));
            graphics.text(font, Component.literal(label), x, y, DISTANCE_COLOR);
        }
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
