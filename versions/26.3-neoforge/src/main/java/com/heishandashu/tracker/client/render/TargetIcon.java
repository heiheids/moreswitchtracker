package com.heishandashu.tracker.client.render;

import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * The tracker reticle: four corner brackets around a centre dot, the same shape as the mod icon.
 *
 * <p>Stateless on purpose. Modules call it, they do not own it, so 遗产追踪 and 玩家追踪 draw the
 * same mark without either of them reaching into the other's state.</p>
 *
 * <p>The gizmo API is world-space only, so the caller sizes the mark with
 * {@link #screenConstantRadius}: because the world radius grows with the viewing distance, the
 * mark keeps the same apparent size on screen however far away it is.</p>
 */
public final class TargetIcon {

    private TargetIcon() {
    }

    /** World radius that makes the reticle look the same size at any distance. */
    public static double screenConstantRadius(Vec3 centre, Vec3 eye, double factor) {
        return Math.max(1.0, eye.distanceTo(centre)) * factor;
    }

    /** Draws the reticle centred on {@code centre}, lying in the plane that faces {@code eye}. */
    public static void draw(Vec3 centre, double radius, Vec3 eye, int color) {
        int dropAxis = dominantAxis(centre, eye);

        for (int axisA = 0; axisA < 3; axisA++) {
            if (axisA == dropAxis) {
                continue;
            }
            for (int axisB = axisA + 1; axisB < 3; axisB++) {
                if (axisB == dropAxis) {
                    continue;
                }
                for (int signA = -1; signA <= 1; signA += 2) {
                    for (int signB = -1; signB <= 1; signB += 2) {
                        Vec3 corner = offset(centre, axisA, signA * radius, axisB, signB * radius);
                        // short "L" arms pointing inward
                        Gizmos.line(corner,
                                offset(centre, axisA, signA * radius * 0.45, axisB, signB * radius),
                                color).setAlwaysOnTop();
                        Gizmos.line(corner,
                                offset(centre, axisA, signA * radius, axisB, signB * radius * 0.45),
                                color).setAlwaysOnTop();
                    }
                }
            }
        }

        Gizmos.point(centre, color, (float) (radius * 0.30)).setAlwaysOnTop();
    }

    /** Axis (0=x, 1=y, 2=z) pointing most directly at the viewer; the icon loses that one. */
    private static int dominantAxis(Vec3 centre, Vec3 eye) {
        Vec3 delta = eye.subtract(centre);
        double ax = Math.abs(delta.x);
        double ay = Math.abs(delta.y);
        double az = Math.abs(delta.z);
        if (ax >= ay && ax >= az) {
            return 0;
        }
        return ay >= az ? 1 : 2;
    }

    private static Vec3 offset(Vec3 base, int axisA, double amountA, int axisB, double amountB) {
        double x = base.x + (axisA == 0 ? amountA : 0.0) + (axisB == 0 ? amountB : 0.0);
        double y = base.y + (axisA == 1 ? amountA : 0.0) + (axisB == 1 ? amountB : 0.0);
        double z = base.z + (axisA == 2 ? amountA : 0.0) + (axisB == 2 ? amountB : 0.0);
        return new Vec3(x, y, z);
    }
}
