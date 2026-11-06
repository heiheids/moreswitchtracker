package com.heishandashu.moreswitches.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screen projection used by the light-level overlay.
 *
 * <p>These pin the two things that silently break an overlay: a position behind the camera being
 * projected to the wrong side of the screen, and the viewport centre not mapping to the screen
 * centre.</p>
 */
class ScreenProjectionTest {

    private static final double FOV = 70.0D;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    /** Eye at the origin looking toward -Z, i.e. "north" in Minecraft's axes. */
    private static ScreenProjection.Point at(double wx, double wy, double wz) {
        return ScreenProjection.project(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, -1.0D,
                wx, wy, wz, FOV, WIDTH, HEIGHT);
    }

    @Test
    void whatIsStraightAheadLandsInTheCentre() {
        ScreenProjection.Point point = at(0.0D, 0.0D, -10.0D);

        assertEquals(WIDTH / 2.0D, point.x(), 0.001D);
        assertEquals(HEIGHT / 2.0D, point.y(), 0.001D);
    }

    @Test
    void somethingToTheRightLandsRightOfCentre() {
        ScreenProjection.Point point = at(2.0D, 0.0D, -10.0D);

        assertTrue(point.x() > WIDTH / 2.0D, "a block to the right must project to the right half");
        assertEquals(HEIGHT / 2.0D, point.y(), 0.001D);
    }

    @Test
    void somethingAboveLandsAboveCentre() {
        ScreenProjection.Point point = at(0.0D, 2.0D, -10.0D);

        assertTrue(point.y() < HEIGHT / 2.0D, "screen y grows downward, so up is a smaller y");
    }

    @Test
    void behindTheCameraHasNoScreenPosition() {
        ScreenProjection.Point point = at(0.0D, 0.0D, 10.0D);

        assertEquals(ScreenProjection.MISS, point);
        assertFalse(ScreenProjection.onScreen(point, WIDTH, HEIGHT, 0));
    }

    @Test
    void aWiderFieldOfViewPullsThingsCloserToTheCentre() {
        ScreenProjection.Point narrow = ScreenProjection.project(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, -1.0D, 3.0D, 0.0D, -10.0D, 30.0D, WIDTH, HEIGHT);
        ScreenProjection.Point wide = ScreenProjection.project(
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, -1.0D, 3.0D, 0.0D, -10.0D, 110.0D, WIDTH, HEIGHT);

        assertTrue(wide.x() < narrow.x(), "the same block sits nearer the centre in a wider view");
        assertTrue(wide.x() > WIDTH / 2.0D, "but it is still on the right");
    }

    @Test
    void theOnScreenTestRejectsPointsOutsideTheViewport() {
        assertTrue(ScreenProjection.onScreen(new ScreenProjection.Point(10.0D, 10.0D), WIDTH, HEIGHT, 0));
        assertFalse(ScreenProjection.onScreen(new ScreenProjection.Point(-5.0D, 10.0D), WIDTH, HEIGHT, 0));
        assertTrue(ScreenProjection.onScreen(new ScreenProjection.Point(-5.0D, 10.0D), WIDTH, HEIGHT, 24),
                "the margin keeps half-drawn glyphs at the edge");
    }
}
