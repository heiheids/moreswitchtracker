package com.heishandashu.tracker.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Projects a world position onto the HUD with the projection the frame was drawn with.
 *
 * <p>1.21.1 has no public accessor for the current projection, so the world pass hands it over with
 * {@link #capture()} while it is still the level's projection. Rebuilding a pinhole from the eye
 * and {@code options.fov()} instead ignores view bobbing and FOV effects, which is what made the
 * markers drift and stutter while the player walked.</p>
 */
public final class CameraProjection {

    private static final Matrix4f PROJECTION = new Matrix4f();

    private static boolean ready;

    private CameraProjection() {
    }

    /** Called from the world pass, where {@code RenderSystem} still holds the level projection. */
    public static void capture() {
        PROJECTION.set(RenderSystem.getProjectionMatrix());
        ready = true;
    }

    /**
     * @param out receives {@code {screenX, screenY}} in GUI units
     * @return false when the point is behind the camera or outside the view
     */
    public static boolean project(Minecraft minecraft, Vec3 point, int width, int height, float[] out) {
        if (!ready) {
            return false;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Matrix4f viewProjection = new Matrix4f(PROJECTION)
                .mul(new Matrix4f().rotation(new Quaternionf(camera.rotation()).conjugate()));

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
        return minecraft.gameRenderer.getMainCamera().getPosition();
    }
}
