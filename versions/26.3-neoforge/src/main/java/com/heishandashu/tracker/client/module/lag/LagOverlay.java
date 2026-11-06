package com.heishandashu.tracker.client.module.lag;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.EdgeArrow;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Marks a highlighted lag hot spot: a reticle in the world, an edge arrow when it is not on screen.
 *
 * <p>Same drawing primitives as the other two tracking features ({@link TargetIcon} for the mark,
 * {@link EdgeArrow} for the off-screen case, {@link ScreenProjection} for the maths), because a hot
 * spot is just a coordinate like a death point.</p>
 */
public final class LagOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "lag_arrows");

    private static final int MARK_COLOR = 0xFF6FD8FF;
    private static final int TEXT_COLOR = 0xFFCDEFFF;
    private static final double SCREEN_FACTOR = 0.035;
    private static final double NEAR_LIMIT = 24.0;
    private static final double ANCHOR_DISTANCE = 12.0;
    private static final double ARROW_MARGIN = 18.0;

    private static boolean warned;

    private LagOverlay() {
    }

    public static void register() {
        TrackerClientEvents.onGizmos(() -> {
            if (!enabled()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer self = minecraft.player;
            if (level == null || self == null || LagState.highlights().isEmpty()) {
                return;
            }
            try {
                Vec3 eye = self.getEyePosition();
                Vec3 view = self.getViewVector(1.0f);
                String dimension = level.dimension().identifier().toString();

                for (LagState.Spot spot : LagState.highlights()) {
                    if (!spot.dimension().equals(dimension)) {
                        continue;
                    }
                    Vec3 target = new Vec3(spot.x(), spot.y(), spot.z());
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
                            continue; // behind: the HUD arrow covers it
                        }
                        drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                    }

                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                    TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);

                    String caption = LagState.label(spot.id()) + "  " + spot.name()
                            + "  " + distanceText(distance)
                            + "  " + LagState.clock(spot.remaining());
                    Gizmos.billboardText(caption, drawAt.add(0.0, radius * 1.9, 0.0),
                                    TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.85f))
                            .setAlwaysOnTop();
                }
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] lag overlay could not draw", t);
                }
            }
        });

        TrackerClientEvents.onHudLayer(HUD_ID, LagOverlay::renderArrows);
    }

    private static String distanceText(double distance) {
        return Component.translatable("tracker.players.distance", String.format("%.0f", distance)).getString();
    }

    private static void renderArrows(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null || LagState.highlights().isEmpty()) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        String dimension = level.dimension().identifier().toString();

        for (LagState.Spot spot : LagState.highlights()) {
            if (!spot.dimension().equals(dimension)) {
                continue;
            }
            ScreenProjection.Projected projected = ScreenProjection.project(eye, forward, fov, width, height,
                    new Vec3(spot.x(), spot.y(), spot.z()), ARROW_MARGIN);
            if (projected.onScreen()) {
                continue;
            }
            String label = LagState.label(spot.id()) + "  " + distanceText(projected.distance());
            EdgeArrow.draw(graphics, minecraft.font, width, height,
                    projected.screenX(), projected.screenY(),
                    projected.dirX(), projected.dirY(), label, MARK_COLOR);
        }
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.LAG_SOURCE_TRACKING);
    }
}
