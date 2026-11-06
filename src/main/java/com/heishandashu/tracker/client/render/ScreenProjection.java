package com.heishandashu.tracker.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * Projects a world position onto the HUD.
 *
 * <p>Plain pinhole projection: an orthonormal basis from the view vector, then
 * {@code tan(angle)/tan(halfFov)} in units of half the screen. Kept free of mod state so every
 * feature that can point at something the player cannot see shares one implementation.</p>
 */
public final class ScreenProjection {

    /**
     * @param screenX  clamped screen position in GUI units
     * @param screenY  clamped screen position in GUI units
     * @param dirX     -1..1 horizontal direction to the target (screen space)
     * @param dirY     -1..1 vertical direction to the target (screen space, + is up)
     * @param distance real distance in blocks
     * @param onScreen true when the target is in front and inside the view
     */
    public record Projected(double screenX, double screenY, double dirX, double dirY,
                            double distance, boolean onScreen) {
    }

    private ScreenProjection() {
    }

    public static Projected project(Vec3 eye, Vec3 forward, double fovY,
                                    int screenWidth, int screenHeight, Vec3 target, double margin) {
        Vec3 f = forward.normalize();
        Vec3 right = new Vec3(-f.z, 0.0, f.x);
        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 up = right.cross(f);

        Vec3 d = target.subtract(eye);
        double xCam = d.dot(right);
        double yCam = d.dot(up);
        double zCam = d.dot(f);
        double distance = d.length();

        double halfFovY = Math.toRadians(fovY) / 2.0;
        double tanY = Math.tan(halfFovY);
        double tanX = tanY * ((double) screenWidth / Math.max(1, screenHeight));

        double nx;
        double ny;
        boolean inFront = zCam > 1.0E-3;
        if (inFront) {
            nx = (xCam / zCam) / tanX;
            ny = (yCam / zCam) / tanY;
        } else {
            // Behind the player there is no projection: push the arrow to the side the target
            // genuinely is, with the vertical offset from its elevation.
            nx = xCam >= 0.0 ? 1.0 : -1.0;
            double flat = Math.hypot(xCam, yCam);
            ny = flat < 1.0E-6 ? 0.0 : Math.max(-1.0, Math.min(1.0, yCam / flat));
        }

        boolean onScreen = inFront && Math.abs(nx) <= 1.0 && Math.abs(ny) <= 1.0;

        double scale = Math.max(Math.abs(nx), Math.abs(ny));
        if (scale > 1.0) {
            nx /= scale;
            ny /= scale;
        }

        double halfW = screenWidth / 2.0;
        double halfH = screenHeight / 2.0;
        return new Projected(halfW + nx * Math.max(0.0, halfW - margin),
                halfH - ny * Math.max(0.0, halfH - margin),
                nx, ny, distance, onScreen);
    }
}
