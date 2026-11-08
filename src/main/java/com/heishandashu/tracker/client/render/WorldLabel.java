package com.heishandashu.tracker.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/**
 * Screen-space labels for world targets.
 *
 * <p>26.3 could draw text in the world with {@code Gizmos.billboardText}. 1.21.1 has no such
 * API, so every "floating caption" in this build is drawn as a HUD label at the projected screen
 * position instead -- same information, positioned where the target appears.</p>
 *
 * <p>Labels are only drawn when the target is actually on screen; off-screen targets use
 * {@link EdgeArrow} instead, which already carries its own label.</p>
 */
public final class WorldLabel {

    /**
     * Height kept clear at the bottom of the screen: the hotbar, plus the health, armour and
     * hunger rows stacked above it. Measured against a screenshot: at 44 the label still landed
     * across the hearts, because the armour/hunger rows sit higher than the hotbar alone.
     */
    private static final int BOTTOM_RESERVED = 68;

    /** Screen-constant reticle radius factor, matching {@code PointHighlight.SCREEN_FACTOR}. */
    private static final double MARK_SCREEN_FACTOR = 0.035;

    /** Gap between the top of the reticle and the bottom of the label, in GUI pixels. */
    private static final int LABEL_GAP = 4;

    private WorldLabel() {
    }

    /**
     * Draws {@code text} at the on-screen position of {@code target}.
     *
     * @return true when the label was drawn (target on screen), false when it was skipped
     */
    public static boolean draw(GuiGraphics graphics, Vec3 eye, Vec3 forward, double fov,
                               Vec3 target, String text, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        ScreenProjection.Projected projected =
                ScreenProjection.project(eye, forward, fov, width, height, target, 0.0);
        if (!projected.onScreen()) {
            return false;
        }
        int textWidth = minecraft.font.width(text);
        int x = (int) Math.round(projected.screenX()) - textWidth / 2;

        // The label used to start at the target itself, so it was drawn straight across the
        // reticle. Lift it by the reticle's on-screen radius plus the text height instead, which
        // puts the caption just above the mark at any FOV or window size.
        double tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);
        int markRadius = (int) Math.round(MARK_SCREEN_FACTOR * (height / 2.0)
                / Math.max(1.0E-4, tanHalfFov));
        int y = (int) Math.round(projected.screenY())
                - markRadius - LABEL_GAP - minecraft.font.lineHeight;
        // Keep it inside the screen so a label near the edge is not clipped away, and keep it
        // clear of the bottom strip: a label drawn over the hotbar and health is unreadable and
        // looks broken (the target is simply below the crosshair, which is normal).
        int bottomLimit = height - BOTTOM_RESERVED;
        x = Math.max(2, Math.min(width - textWidth - 2, x));
        y = Math.max(2, Math.min(bottomLimit, y));
        graphics.drawString(minecraft.font, text, x, y, color, true);
        return true;
    }
}
