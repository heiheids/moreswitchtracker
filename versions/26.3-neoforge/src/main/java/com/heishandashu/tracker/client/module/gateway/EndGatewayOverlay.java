package com.heishandashu.tracker.client.module.gateway;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.module.gateway.EndGatewayTrackingClient.Marked;
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
 * Draws the marked end gateway: a purple reticle, its distance, and an edge arrow when it is off
 * screen.
 *
 * <p>A gateway is a place rather than an entity, so the mark is a fixed coordinate; the same shared
 * primitives as the other features are used for the drawing.</p>
 */
public final class EndGatewayOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "gateway_arrows");

    private static final int MARK_COLOR = 0xFFC98BFF;
    private static final int TEXT_COLOR = 0xFFE9D6FF;
    private static final double SCREEN_FACTOR = 0.04;
    private static final double NEAR_LIMIT = 24.0;
    private static final double ANCHOR_DISTANCE = 12.0;
    private static final double ARROW_MARGIN = 18.0;

    private static boolean warned;

    private EndGatewayOverlay() {
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
            Marked mark = EndGatewayTrackingClient.marked(EndGatewayTrackingClient.now());
            if (mark == null || !mark.dimension().equals(level.dimension().identifier().toString())) {
                return;
            }
            try {
                Vec3 target = EndGatewayTrackingClient.position(mark);
                Vec3 eye = self.getEyePosition();
                Vec3 view = self.getViewVector(1.0f);
                double distance = eye.distanceTo(target);
                if (distance < 0.05) {
                    return;
                }

                Vec3 drawAt;
                double lineOffset;
                if (distance <= NEAR_LIMIT) {
                    drawAt = target;
                    lineOffset = 1.2;
                } else {
                    Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                    if (direction.dot(view) <= 0.05) {
                        return; // behind: the HUD arrow covers it
                    }
                    drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                    lineOffset = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR) * 1.9;
                }

                double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);

                String caption = Component.translatable("tracker.gateway.label").getString()
                        + "  " + Component.translatable("tracker.players.distance",
                                String.format("%.0f", distance)).getString()
                        + "  " + clock(mark.remaining(EndGatewayTrackingClient.now()));
                Gizmos.billboardText(caption, drawAt.add(0.0, lineOffset, 0.0),
                                TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.85f))
                        .setAlwaysOnTop();
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] gateway overlay could not draw", t);
                }
            }
        });

        TrackerClientEvents.onHudLayer(HUD_ID, EndGatewayOverlay::renderArrow);
    }

    private static void renderArrow(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            return;
        }
        Marked mark = EndGatewayTrackingClient.marked(EndGatewayTrackingClient.now());
        if (mark == null || !mark.dimension().equals(level.dimension().identifier().toString())) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        ScreenProjection.Projected projected = ScreenProjection.project(
                self.getEyePosition(), self.getViewVector(1.0f), minecraft.options.fov().get(),
                width, height, EndGatewayTrackingClient.position(mark), ARROW_MARGIN);
        if (projected.onScreen()) {
            return;
        }
        String label = Component.translatable("tracker.gateway.label").getString()
                + "  " + Component.translatable("tracker.players.distance",
                        String.format("%.0f", projected.distance())).getString();
        EdgeArrow.draw(graphics, minecraft.font, width, height,
                projected.screenX(), projected.screenY(),
                projected.dirX(), projected.dirY(), label, MARK_COLOR);
    }

    private static String clock(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.END_GATEWAY_TRACKING);
    }
}
