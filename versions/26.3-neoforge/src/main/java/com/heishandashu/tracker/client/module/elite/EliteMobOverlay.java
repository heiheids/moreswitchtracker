package com.heishandashu.tracker.client.module.elite;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.module.elite.EliteMobTrackingClient.Highlight;
import com.heishandashu.tracker.client.render.EdgeArrow;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.elite.EliteMobTrackingModule;
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

import java.util.List;

/**
 * Draws a marked elite mob: a red reticle in the world, its health while the player is close, and
 * an edge arrow when it is off screen.
 *
 * <p>Uses the shared primitives ({@link TargetIcon}, {@link EdgeArrow}, {@link ScreenProjection}) so
 * its arrows behave exactly like the other tracking features'.</p>
 */
public final class EliteMobOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "elite_arrows");

    private static final int MARK_COLOR = 0xFFFF6B5A;
    private static final int TEXT_COLOR = 0xFFFFD9D2;
    private static final int HEALTH_COLOR = 0xFFFFB4A6;
    private static final double SCREEN_FACTOR = 0.035;
    private static final double NEAR_LIMIT = 24.0;
    private static final double ANCHOR_DISTANCE = 12.0;
    private static final double ARROW_MARGIN = 18.0;

    private static boolean warned;

    private EliteMobOverlay() {
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

                for (Highlight elite : EliteMobTrackingClient.highlights()) {
                    if (!elite.dimension().equals(dimension)) {
                        continue;
                    }
                    Vec3 target = new Vec3(elite.x(), elite.y(), elite.z());
                    double distance = eye.distanceTo(target);
                    if (distance < 0.05) {
                        continue;
                    }

                    Vec3 drawAt;
                    double lineOffset;
                    if (distance <= NEAR_LIMIT) {
                        drawAt = target.add(0.0, 2.4, 0.0);
                        lineOffset = 0.4;
                    } else {
                        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind: the HUD arrow covers it
                        }
                        drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                        lineOffset = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR) * 1.9;
                    }

                    if (distance <= NEAR_LIMIT) {
                        double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                        TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);
                        lineOffset = radius * 1.9;
                    }

                    // Health while close enough to matter, plus the highlight countdown.
                    if (distance <= EliteMobTrackingModule.OVERHEAD_RANGE) {
                        String health = Component.translatable("tracker.elite.health",
                                String.format("%.0f", elite.health()),
                                String.format("%.0f", elite.maxHealth())).getString();
                        Gizmos.billboardText(health, drawAt.add(0.0, lineOffset, 0.0),
                                        TextGizmo.Style.forColorAndCentered(HEALTH_COLOR).withScale(0.8f))
                                .setAlwaysOnTop();
                        lineOffset += 0.55;
                    }

                    String caption = elite.name() + "  " + distanceText(distance)
                            + "  " + EliteMobTrackingClient.clock(elite.remaining());
                    Gizmos.billboardText(caption, drawAt.add(0.0, lineOffset, 0.0),
                                    TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.85f))
                            .setAlwaysOnTop();
                }
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] elite overlay could not draw", t);
                }
            }
        });

        TrackerClientEvents.onHudLayer(HUD_ID, EliteMobOverlay::renderArrows);
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
        if (self == null || level == null) {
            return;
        }
        List<Highlight> highlights = EliteMobTrackingClient.highlights();
        if (highlights.isEmpty()) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        String dimension = level.dimension().identifier().toString();

        for (Highlight elite : highlights) {
            if (!elite.dimension().equals(dimension)) {
                continue;
            }
            ScreenProjection.Projected projected = ScreenProjection.project(eye, forward, fov, width, height,
                    new Vec3(elite.x(), elite.y(), elite.z()), ARROW_MARGIN);
            if (projected.onScreen()) {
                continue;
            }
            String label = elite.name() + "  " + distanceText(projected.distance());
            EdgeArrow.draw(graphics, minecraft.font, width, height,
                    projected.screenX(), projected.screenY(),
                    projected.dirX(), projected.dirY(), label, MARK_COLOR);
        }
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.ELITE_MOB_TRACKING);
    }
}
