package com.heishandashu.tracker.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The 1.21.1 stand-in for 26.3's {@code Gizmos} API.
 *
 * <p>26.3 ships a world-space gizmo API with a through-wall ({@code setAlwaysOnTop}) channel.
 * 1.21.1 has <b>no such thing</b>, so every highlight has to be drawn by hand: build a vertex
 * buffer on {@link RenderType#lines()} (or {@code debugLineStrip}, which ignores depth), place it
 * in camera space, and submit it.</p>
 *
 * <p>This class owns that one job so the feature renderers above it stay readable and stay
 * identical in shape to their 26.3 siblings. All coordinates are absolute world coordinates; the
 * camera offset is applied here.</p>
 *
 * <p><b>Depth handling:</b> {@code RenderType.lines()} writes depth, so a line behind a wall is
 * hidden. Callers that want the 26.3 "always on top" look use {@link #beginThroughWalls}, which
 * picks a line type with depth writing disabled.</p>
 */
public final class WorldPainter {

    /** Line width used for the through-wall channel, in screen pixels. */
    private static final double THROUGH_WALL_LINE_WIDTH = 2.0;

    /** One buffer the caller writes into, already positioned in camera space. */
    public interface Batch extends AutoCloseable {
        /** Draws an axis-aligned box outline. */
        void box(AABB box, float red, float green, float blue, float alpha);

        /** Draws a line between two world positions. */
        void line(Vec3 from, Vec3 to, float red, float green, float blue, float alpha);

        @Override
        void close();
    }

    private WorldPainter() {
    }

    /**
     * Opens a batch that ignores the depth buffer, i.e. draws through walls.
     *
     * @param buffers the buffer source the render event handed us
     * @param pose    the camera-aligned pose stack from the render event
     */
    public static Batch beginThroughWalls(MultiBufferSource buffers, PoseStack pose) {
        return new LineBatch(buffers, pose, true);
    }

    /** Opens a depth-tested batch (normal occlusion). */
    public static Batch beginOccluded(MultiBufferSource buffers, PoseStack pose) {
        return new LineBatch(buffers, pose, false);
    }

    private static final class LineBatch implements Batch {

        private final VertexConsumer consumer;
        private final PoseStack pose;
        /** Camera position, subtracted so vertex coordinates stay small and precise. */
        private final double camX;
        private final double camY;
        private final double camZ;

        LineBatch(MultiBufferSource buffers, PoseStack pose, boolean throughWalls) {
            Minecraft minecraft = Minecraft.getInstance();
            Camera camera = minecraft.gameRenderer.getMainCamera();
            Vec3 cam = camera.getPosition();
            this.camX = cam.x;
            this.camY = cam.y;
            this.camZ = cam.z;
            this.pose = pose;
            // debugLineStrip keeps depth testing off, which is the 1.21.1 equivalent of 26.3's
            // setAlwaysOnTop(). Its argument is a line width in pixels.
            this.consumer = buffers.getBuffer(throughWalls
                    ? RenderType.debugLineStrip(THROUGH_WALL_LINE_WIDTH)
                    : RenderType.lines());
        }

        @Override
        public void box(AABB box, float red, float green, float blue, float alpha) {
            this.pose.pushPose();
            this.pose.translate(-this.camX, -this.camY, -this.camZ);
            LevelRenderer.renderLineBox(this.pose, this.consumer,
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    red, green, blue, alpha);
            this.pose.popPose();
        }

        @Override
        public void line(Vec3 from, Vec3 to, float red, float green, float blue, float alpha) {
            // A zero-thickness box is the cheapest correct way to emit a single line segment with
            // the normal/position layout that RenderType.lines() expects.
            AABB thin = new AABB(from.x, from.y, from.z, to.x, to.y, to.z);
            this.pose.pushPose();
            this.pose.translate(-this.camX, -this.camY, -this.camZ);
            LevelRenderer.renderLineBox(this.pose, this.consumer,
                    thin.minX, thin.minY, thin.minZ, thin.maxX, thin.maxY, thin.maxZ,
                    red, green, blue, alpha);
            this.pose.popPose();
        }

        @Override
        public void close() {
            // End-of-batch render types are flushed by the buffer source; nothing to release.
        }
    }
}
