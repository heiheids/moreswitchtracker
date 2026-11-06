package com.heishandashu.tracker.client.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The tracker reticle: four corner brackets around a centre dot.
 *
 * <p>26.3 draws this with {@code Gizmos.line}. 1.21.1 has no gizmo API, so it goes through
 * {@link WorldPainter} instead. The caller still supplies a world radius; use
 * {@link #screenConstantRadius} so the mark keeps the same apparent size at any distance.</p>
 */
public final class TargetIcon {

    private TargetIcon() {
    }

    /** World radius that makes the reticle look the same size at any distance. */
    public static double screenConstantRadius(Vec3 centre, Vec3 eye, double factor) {
        return Math.max(1.0, eye.distanceTo(centre)) * factor;
    }

    /** Draws the reticle centred on {@code centre}, lying in the plane that faces {@code eye}. */
    public static void draw(WorldPainter.Batch batch, Vec3 centre, double radius, Vec3 eye,
                            float red, float green, float blue, float alpha) {
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
                        Vec3 armA = offset(centre, axisA, signA * radius * 0.45, axisB, signB * radius);
                        Vec3 armB = offset(centre, axisA, signA * radius, axisB, signB * radius * 0.45);
                        batch.line(corner, armA, red, green, blue, alpha);
                        batch.line(corner, armB, red, green, blue, alpha);
                    }
                }
            }
        }

        // Centre dot: a tiny cube reads as a point at any distance without needing a line strip.
        double dot = Math.max(radius * 0.12, 0.02);
        batch.box(new AABB(centre.x - dot, centre.y - dot, centre.z - dot,
                centre.x + dot, centre.y + dot, centre.z + dot), red, green, blue, alpha);
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
