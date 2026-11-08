package com.heishandashu.tracker.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Projects a world position onto the HUD with the camera's own matrix.
 *
 * <p>Rebuilding a pinhole from the player's eye and {@code options.fov()} is subtly wrong: the
 * world is drawn with view bobbing, FOV effects and a possible third-person offset, so a marker
 * placed by a hand-rolled projection slides away from its target as soon as the player moves. Using
 * the matrix the frame was actually drawn with cannot disagree with it.</p>
 */
public final class CameraProjection {

    private CameraProjection() {
    }

    /**
     * @param out receives {@code {screenX, screenY}} in GUI units
     * @return false when the point is behind the camera or outside the view
     */
    public static boolean project(Minecraft minecraft, Vec3 point, int width, int height, float[] out) {
        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());

        Vector4f clip = viewProjection.transform(new Vector4f(
                (float) (point.x - eye.x),
                (float) (point.y - eye.y),
                (float) (point.z - eye.z),
                1.0F));
        if (clip.w <= 1.0E-4F) {
            return false;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (Math.abs(ndcX) > 1.0F || Math.abs(ndcY) > 1.0F) {
            return false;
        }
        out[0] = (ndcX * 0.5F + 0.5F) * width;
        out[1] = (0.5F - ndcY * 0.5F) * height;
        return true;
    }

    /** Camera position, i.e. the point the projection above is relative to. */
    public static Vec3 eye(Minecraft minecraft) {
        return minecraft.gameRenderer.mainCamera().position();
    }
}
