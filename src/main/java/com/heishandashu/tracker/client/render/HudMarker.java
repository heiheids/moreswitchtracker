package com.heishandashu.tracker.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The tracking marker, drawn on the HUD instead of in the world.
 *
 * <p>World-space markers are placed from the player's tick position, which only updates twenty
 * times a second while the camera moves every frame — so they visibly stutter as soon as the player
 * walks. Drawing them on the HUD from the camera matrix removes that entirely, and being in GUI
 * pixels makes the shape exact at any GUI scale.</p>
 *
 * <p>The shape is a square outline with a dot in the middle: readable at six pixels, and it does
 * not turn to mush the way thin diagonal brackets do.</p>
 */
public final class HudMarker {

    private HudMarker() {
    }

    /** Square outline of the given half-size, one pixel thick. */
    public static void square(GuiGraphics graphics, int centerX, int centerY,
                              int radius, int color) {
        int left = centerX - radius;
        int right = centerX + radius;
        int top = centerY - radius;
        int bottom = centerY + radius;
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    /** Filled centre dot; {@code half} is the half-size in pixels. */
    public static void dot(GuiGraphics graphics, int centerX, int centerY,
                           int half, int color) {
        graphics.fill(centerX - half, centerY - half, centerX + half, centerY + half, color);
    }

    /** Square plus dot, the default marker. */
    public static void mark(GuiGraphics graphics, int centerX, int centerY,
                            int radius, int color) {
        square(graphics, centerX, centerY, radius, color);
        dot(graphics, centerX, centerY, Math.max(1, radius / 4), color);
    }

    /** Horizontally centred text, clamped inside the screen. */
    public static void centeredText(GuiGraphics graphics, Font font, String text,
                                    int centerX, int y, int width, int height, int color) {
        int textWidth = font.width(text);
        int x = Math.max(2, Math.min(Math.max(2, width - textWidth - 2), centerX - textWidth / 2));
        int clampedY = Math.max(2, Math.min(Math.max(2, height - font.lineHeight - 2), y));
        graphics.drawString(font, text, x, clampedY, color, true);
    }
}
