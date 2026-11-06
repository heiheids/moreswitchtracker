package com.heishandashu.tracker.client.module.persistent;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.PickedMobNetworking;
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
 * Draws the marker for a highlighted mob: a reticle in the world, or an edge arrow when it is not
 * on screen.
 *
 * <p>Same two techniques as 玩家追踪, because the constraints are identical: the world marker is
 * drawn along the view direction at a fixed short distance when the mob is far (so it can never be
 * clipped by the far plane), and {@link ScreenProjection} handles the off-screen case.</p>
 */
public final class PickedMobHighlightRenderer {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "picked_mob_arrows");

    private static final int MARK_COLOR = 0xFFFFC24A;
    private static final int TEXT_COLOR = 0xFFFFE6B0;
    private static final double SCREEN_FACTOR = 0.035;
    private static final double NEAR_LIMIT = 24.0;
    private static final double ANCHOR_DISTANCE = 12.0;
    private static final double MARGIN = 18.0;
    private static final int ARROW_SIZE = 7;

    private static boolean warned;

    private PickedMobHighlightRenderer() {
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
                Vec3 eye = self.getEyePosition();
                Vec3 view = self.getViewVector(1.0f);
                String dimension = level.dimension().identifier().toString();

                for (PickedMobHighlightState.Highlight highlight : PickedMobHighlightState.highlights()) {
                    PickedMobNetworking.PickedMobSnap snap = PickedMobHighlightState.record(highlight.uuid());
                    if (snap == null || !snap.dimension().equals(dimension)) {
                        continue;
                    }
                    Vec3 target = PickedMobHighlightState.position(snap);
                    double distance = eye.distanceTo(target);
                    if (distance < 0.05) {
                        continue;
                    }

                    Vec3 drawAt;
                    if (distance <= NEAR_LIMIT) {
                        drawAt = target;
                    } else {
                        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind the player: the HUD arrow covers this
                        }
                        drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                    }

                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                    TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);

                    String caption = highlight.name() + "  " + distanceText(distance)
                            + "  " + captionExtra(snap);
                    Gizmos.billboardText(caption, drawAt.add(0.0, radius * 1.9, 0.0),
                                    TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.9f))
                            .setAlwaysOnTop();
                }
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] picked-mob highlight could not draw", t);
                }
            }
        });

        TrackerClientEvents.onHudLayer(HUD_ID, PickedMobHighlightRenderer::renderArrows);
    }

    private static String captionExtra(PickedMobNetworking.PickedMobSnap snap) {
        return PickedMobHighlightState.itemName(snap).getString() + " x" + snap.count();
    }

    private static String distanceText(double distance) {
        return Component.translatable("tracker.players.distance",
                String.format("%.0f", distance)).getString();
    }

    /** Edge arrows for highlighted mobs that are not on screen. */
    private static void renderArrows(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null || PickedMobHighlightState.highlights().isEmpty()) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        String dimension = level.dimension().identifier().toString();

        for (PickedMobHighlightState.Highlight highlight : PickedMobHighlightState.highlights()) {
            PickedMobNetworking.PickedMobSnap snap = PickedMobHighlightState.record(highlight.uuid());
            if (snap == null || !snap.dimension().equals(dimension)) {
                continue;
            }
            ScreenProjection.Projected projected = ScreenProjection.project(
                    eye, forward, fov, width, height, PickedMobHighlightState.position(snap), MARGIN);
            if (projected.onScreen()) {
                continue;
            }
            drawArrow(graphics, projected.screenX(), projected.screenY(),
                    projected.dirX(), projected.dirY());
            String label = highlight.name() + "  " + distanceText(projected.distance())
                    + "  " + PlayerHighlightRendererTime.format(highlight.remaining());
            int textWidth = font.width(label);
            int x = Math.max(2, Math.min(width - textWidth - 2,
                    (int) Math.round(projected.screenX()) - textWidth / 2));
            int base = (int) Math.round(projected.screenY());
            int y = Math.max(2, Math.min(height - 10, base + (base > height / 2 ? -14 : 10)));
            graphics.text(font, Component.literal(label), x, y, TEXT_COLOR);
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
        return TrackerModules.isEnabled(ModuleIds.PERSISTENT_MOB_TRACKING);
    }

    /** Small local copy of the tick formatter so this module does not reach into 玩家追踪. */
    private static final class PlayerHighlightRendererTime {
        private PlayerHighlightRendererTime() {
        }

        static String format(int ticks) {
            int totalSeconds = Math.max(0, ticks) / 20;
            return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
        }
    }
}
