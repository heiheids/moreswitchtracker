package com.heishandashu.tracker.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws one "point at this place" highlight, for both the world pass and the HUD pass.
 *
 * <p>On 26.3 every such feature (遗产追踪 / 玩家追踪 / 流浪商人 / 卡顿 / 精英怪 / 折跃门 /
 * 自定义坐标 / 不消失怪物) was a thin wrapper around the gizmo API, so each one carried its own
 * copy of the same four steps. 1.21.1 has no gizmo API and needs those steps spelled out, so they
 * live here once and the features just say <em>where</em> and <em>what colour</em>.</p>
 *
 * <p>Two passes, because 1.21.1 needs them in different places:</p>
 * <ul>
 *   <li>{@link #world} — brackets/boxes in the world, depth-independent (through walls),</li>
 *   <li>{@link #hud} — the caption and the edge arrow when the target is off screen.</li>
 * </ul>
 */
public final class PointHighlight {

    /** Inside this distance the mark hugs the real position instead of the screen anchor. */
    public static final double NEAR_LIMIT = 24.0;
    /** World-space distance used for the screen-anchored mark when the target is far away. */
    public static final double ANCHOR_DISTANCE = 12.0;
    /** Retained so 26.3-era tuning constants keep the same meaning. */
    public static final double SCREEN_FACTOR = 0.035;
    /** Half-size of the HUD marker square, in GUI pixels. */
    private static final int MARK_RADIUS = 7;
    /** Gap between the marker and the caption above it, in GUI pixels. */
    private static final int LABEL_GAP = 5;

    private PointHighlight() {
    }

    /**
     * Draws the reticle plus an optional box around a target.
     *
     * @param boxHalf half-size of the solid box drawn on the exact spot; 0 draws no box
     */
    public static void world(PoseStack pose, MultiBufferSource buffers, Vec3 target,
                             float red, float green, float blue, float alpha,
                             double boxHalf) {
        // The level's projection is only reachable while it is bound, which is here. The HUD pass
        // reuses it so the marker lands exactly where the frame drew the world.
        CameraProjection.capture();

        if (boxHalf <= 0.0) {
            return;
        }
        WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
        try {
            AABB box = new AABB(target.x - boxHalf, target.y - boxHalf, target.z - boxHalf,
                    target.x + boxHalf, target.y + boxHalf, target.z + boxHalf);
            batch.box(box, red, green, blue, alpha);
        } finally {
            batch.close();
        }
    }

    /**
     * Draws the caption at the target, or an edge arrow when it is outside the view.
     *
     * @return true when something was drawn
     */
    public static boolean hud(GuiGraphics graphics, Vec3 target, String caption, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        float[] screen = new float[2];
        if (CameraProjection.project(minecraft, target, width, height, screen)) {
            int centerX = Math.round(screen[0]);
            int centerY = Math.round(screen[1]);
            HudMarker.mark(graphics, centerX, centerY, MARK_RADIUS, color);
            HudMarker.centeredText(graphics, minecraft.font, caption, centerX,
                    centerY - MARK_RADIUS - LABEL_GAP - minecraft.font.lineHeight,
                    width, height, color);
            return true;
        }

        ScreenProjection.Projected projected = ScreenProjection.project(
                CameraProjection.eye(minecraft), player.getViewVector(1.0f),
                minecraft.options.fov().get(), width, height, target, 18.0);
        EdgeArrow.draw(graphics, minecraft.font, width, height,
                projected.screenX(), projected.screenY(),
                projected.dirX(), projected.dirY(), caption, color);
        return true;
    }

    /** {@code [123, 64, -30]} for captions. */
    public static String coords(double x, double y, double z) {
        return "[" + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + "]";
    }

    /** "距离 37 格", in the client's language. */
    public static String distanceText(double distance) {
        return net.minecraft.network.chat.Component
                .translatable("tracker.players.distance", String.format("%.0f", distance))
                .getString();
    }
}
