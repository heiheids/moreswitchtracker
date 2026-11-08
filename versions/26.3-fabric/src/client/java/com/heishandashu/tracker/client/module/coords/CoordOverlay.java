package com.heishandashu.tracker.client.module.coords;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.CameraProjection;
import com.heishandashu.tracker.client.render.EdgeArrow;
import com.heishandashu.tracker.client.render.HudMarker;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.coords.CustomCoordTrackingModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the custom coordinate: a reticle in the world, an edge arrow when it is off screen, and
 * the distance/coordinate label.
 *
 * <p>Uses the same primitives as every other tracking feature ({@link TargetIcon},
 * {@link EdgeArrow}, {@link ScreenProjection}), so the mark looks and behaves identically. The
 * far-target trick is the same one 玩家追踪 needs: world gizmos are clipped at the camera far
 * plane, so beyond {@link CustomCoordTrackingModule#NEAR_LIMIT} the mark is re-drawn along the
 * view direction at a fixed distance, which keeps it on screen at any range.</p>
 */
public final class CoordOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "custom_coord_arrow");

    /** Cyan-ish, distinct from the purple legacy mark and the pink player mark. */
    private static final int MARK_COLOR = 0xFF4FD2FF;
    private static final int TEXT_COLOR = 0xFFCDEFFF;
    private static final double SCREEN_FACTOR = 0.035;
    private static final double ARROW_MARGIN = 18.0;
    /** Vertical offset of a corner bracket at the exact coordinate, in blocks. */
    private static final double BOX_HALF = 0.6;
    /** Pixels between the reticle centre and the caption above it. */
    private static final int LABEL_GAP = 5;
    /** Half-size of the HUD marker square, in GUI pixels. */
    private static final int MARK_RADIUS = 7;

    private static boolean warned;

    private CoordOverlay() {
    }

    public static void register() {
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            if (!active()) {
                return;
            }
            try {
                drawWorld();
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] custom coordinate could not draw", t);
                }
            }
        });
        HudElementRegistry.addLast(HUD_ID, CoordOverlay::renderArrow);
    }

    /** The point to show, or null when the switch is off / nothing is saved / wrong dimension. */
    private static CoordState.Point visiblePoint(ClientLevel level) {
        if (!active() || level == null) {
            return null;
        }
        CoordState.Point point = CoordState.point();
        if (point == null || !point.dimension().equals(level.dimension().identifier().toString())) {
            return null;
        }
        return point;
    }

    private static void drawWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        CoordState.Point point = visiblePoint(level);
        if (point == null || self == null) {
            return;
        }

        Vec3 eye = self.getEyePosition();
        Vec3 target = new Vec3(point.x(), point.y(), point.z());
        double distance = eye.distanceTo(target);

        // The exact spot gets a small always-on-top box, so it reads as a place rather than a dot.
        AABB box = new AABB(target.x - BOX_HALF, target.y - BOX_HALF, target.z - BOX_HALF,
                target.x + BOX_HALF, target.y + BOX_HALF, target.z + BOX_HALF);
        Gizmos.cuboid(box, GizmoStyle.stroke(MARK_COLOR, 2.0f)).setAlwaysOnTop();


    }

    private static void renderArrow(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        CoordState.Point point = visiblePoint(level);
        if (point == null || self == null) {
            return;
        }

        Vec3 eye = CameraProjection.eye(minecraft);
        Vec3 target = new Vec3(point.x(), point.y(), point.z());
        double distance = eye.distanceTo(target);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        String caption = point.pretty() + "  " + distanceText(distance);

        // On screen the label is placed by hand, a fixed gap above the reticle. A world-space
        // billboard lands beside the mark rather than above it, which is what made the caption
        // sit across the icon.
        float[] screen = new float[2];
        if (CameraProjection.project(minecraft, target, width, height, screen)) {
            int centerX = Math.round(screen[0]);
            int centerY = Math.round(screen[1]);
            HudMarker.mark(graphics, centerX, centerY, MARK_RADIUS, MARK_COLOR);
            HudMarker.centeredText(graphics, minecraft.font, caption, centerX,
                    centerY - MARK_RADIUS - LABEL_GAP - minecraft.font.lineHeight,
                    width, height, TEXT_COLOR);
            return;
        }

        // Off screen: an edge arrow, which is the only thing that can point at it.
        ScreenProjection.Projected projected = ScreenProjection.project(eye, self.getViewVector(1.0f),
                minecraft.options.fov().get(), width, height, target, ARROW_MARGIN);
        EdgeArrow.draw(graphics, minecraft.font, width, height,
                projected.screenX(), projected.screenY(),
                projected.dirX(), projected.dirY(), caption, MARK_COLOR);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    private static String distanceText(double distance) {
        return Component.translatable("tracker.players.distance", String.format("%.0f", distance))
                .getString();
    }

    private static boolean active() {
        return CustomCoordTrackingModule.isActive();
    }
}
