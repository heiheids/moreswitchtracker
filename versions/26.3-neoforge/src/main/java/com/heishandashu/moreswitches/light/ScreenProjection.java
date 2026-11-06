package com.heishandashu.moreswitches.light;

/**
 * Projects a world position onto the screen for the light-level overlay.
 *
 * <p>The overlay is drawn as ordinary screen text. Submitting text into the world render pass
 * produced nothing visible on the newer pipeline, so the numbers are placed by projecting each
 * block's top-centre with plain arithmetic and letting the GUI text renderer draw them. The digits
 * therefore do not depth-test against terrain, which for a light preview reads fine: they are meant
 * to sit on top of the blocks.</p>
 *
 * <p>Everything here is plain numbers. Keeping the arithmetic free of engine types is what makes
 * the projection testable without booting a game, and it means a mistake in the maths shows up as a
 * failed test rather than as text in the wrong place.</p>
 */
public final class ScreenProjection {

    private ScreenProjection() {
    }

    /** A point on screen, or {@link #MISS} when the position is behind the camera. */
    public record Point(double x, double y) {
    }

    /** Returned for positions behind the camera, which have no meaningful screen position. */
    public static final Point MISS = new Point(Double.NaN, Double.NaN);

    /**
     * Projects {@code world} onto a viewport of {@code width} by {@code height}.
     *
     * @param eyeX eye position, X
     * @param eyeY eye position, Y
     * @param eyeZ eye position, Z
     * @param lookX unit view direction, X
     * @param lookY unit view direction, Y
     * @param lookZ unit view direction, Z
     * @param worldX the position to project, X
     * @param worldY the position to project, Y
     * @param worldZ the position to project, Z
     * @param fovDegrees the vertical field of view
     * @return the screen point, or {@link #MISS} when the position is behind the camera
     */
    public static Point project(double eyeX, double eyeY, double eyeZ,
                                double lookX, double lookY, double lookZ,
                                double worldX, double worldY, double worldZ,
                                double fovDegrees, int width, int height) {
        double fx = lookX;
        double fy = lookY;
        double fz = lookZ;
        double length = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (length < 1.0E-9D) {
            return MISS;
        }
        fx /= length;
        fy /= length;
        fz /= length;

        double rx = worldX - eyeX;
        double ry = worldY - eyeY;
        double rz = worldZ - eyeZ;

        // Depth along the view direction. Anything at or behind the eye plane is rejected: the
        // perspective divide would otherwise mirror it to the opposite side of the screen.
        double depth = rx * fx + ry * fy + rz * fz;
        if (depth <= 1.0E-4D) {
            return MISS;
        }

        // right = forward x worldUp, with worldUp = (0, 1, 0). For forward = (0, 0, -1) this is
        // (+1, 0, 0), which is the camera's right; the other order points left.
        double sx = fy * 0.0D - fz * 1.0D;
        double sy = fz * 0.0D - fx * 0.0D;
        double sz = fx * 1.0D - fy * 0.0D;
        double slen = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (slen < 1.0E-8D) {
            // Looking straight up or down, where the usual "right" is undefined; any perpendicular
            // will do.
            sx = fz * 0.0D - fy * 1.0D;
            sy = fx * 1.0D - fz * 0.0D;
            sz = fy * 0.0D - fx * 0.0D;
            slen = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slen < 1.0E-8D) {
                return MISS;
            }
        }
        sx /= slen;
        sy /= slen;
        sz /= slen;

        // up = right x forward, which is the screen's up direction.
        double ux = sy * fz - sz * fy;
        double uy = sz * fx - sx * fz;
        double uz = sx * fy - sy * fx;

        double halfHeight = Math.tan(Math.toRadians(fovDegrees) / 2.0D);
        double aspect = (double) width / (double) height;

        double ndcX = ((rx * sx + ry * sy + rz * sz) / depth) / (halfHeight * aspect);
        double ndcY = ((rx * ux + ry * uy + rz * uz) / depth) / halfHeight;

        double x = (ndcX * 0.5D + 0.5D) * width;
        double y = (0.5D - ndcY * 0.5D) * height;
        return new Point(x, y);
    }

    /** Whether a projected point lies inside the viewport, with a margin so half-glyphs are skipped. */
    public static boolean onScreen(Point point, int width, int height, int margin) {
        if (point == null || Double.isNaN(point.x()) || Double.isNaN(point.y())) {
            return false;
        }
        return point.x() >= -margin && point.x() <= width + margin
                && point.y() >= -margin && point.y() <= height + margin;
    }
}
