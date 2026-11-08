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
        LocalPlayer player = Minecraft.getInstance().player;
        // The camera's interpolated position, not the player's tick position: anchoring on the
        // latter makes every world marker stutter once per tick while the player walks.
        Vec3 eye = player == null
                ? target.add(0.0, 0.0, 8.0)
                : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = eye.distanceTo(target);

        WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
        try {
            if (boxHalf > 0.0) {
                AABB box = new AABB(target.x - boxHalf, target.y - boxHalf, target.z - boxHalf,
                        target.x + boxHalf, target.y + boxHalf, target.z + boxHalf);
                batch.box(box, red, green, blue, alpha);
            }

            // A world gizmo beyond the far plane is clipped, so far targets are re-anchored along
            // the view direction: same screen position, unchanged apparent size, never culled.
            Vec3 drawAt = target;
            if (distance > NEAR_LIMIT) {
                Vec3 direction = target.subtract(eye).scale(1.0 / Math.max(distance, 1.0E-4));
                drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
            }
            double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
            TargetIcon.draw(batch, drawAt, radius, eye, red, green, blue, alpha);
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
        Vec3 eye = player.getEyePosition();
        Vec3 forward = player.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        ScreenProjection.Projected projected =
                ScreenProjection.project(eye, forward, fov, width, height, target, 18.0);
        if (projected.onScreen()) {
            return WorldLabel.draw(graphics, eye, forward, fov, target, caption, color);
        }
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
