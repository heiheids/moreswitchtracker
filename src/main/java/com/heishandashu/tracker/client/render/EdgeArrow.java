package com.heishandashu.tracker.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The screen-edge arrow plus its label, used by every feature that can point at something the
 * player cannot see.
 *
 * <p>Stateless and module-agnostic, so the tracking features share one implementation instead of
 * each growing its own copy.</p>
 */
public final class EdgeArrow {

    /** How big the triangle is, in GUI pixels. */
    private static final int SIZE = 7;

    private EdgeArrow() {
    }

    public static void draw(GuiGraphics graphics, Font font, int screenWidth, int screenHeight,
                            double x, double y, double dirX, double dirY, String label, int color) {
        int cx = (int) Math.round(x);
        int cy = (int) Math.round(y);

        if (Math.abs(dirX) >= Math.abs(dirY)) {
            int sign = dirX >= 0 ? 1 : -1;
            for (int i = 0; i < SIZE; i++) {
                int edge = cx - sign * i;
                int half = i + 1;
                graphics.fill(Math.min(edge, edge + sign), cy - half,
                        Math.max(edge, edge + sign), cy + half, color);
            }
        } else {
            // Screen y grows downwards, so a target above the view points up.
            int sign = dirY >= 0 ? -1 : 1;
            for (int i = 0; i < SIZE; i++) {
                int edge = cy - sign * i;
                int half = i + 1;
                graphics.fill(cx - half, Math.min(edge, edge + sign),
                        cx + half, Math.max(edge, edge + sign), color);
            }
        }

        if (label == null || label.isEmpty()) {
            return;
        }
        int textWidth = font.width(label);
        int textX = Math.max(2, Math.min(screenWidth - textWidth - 2, cx - textWidth / 2));
        int textY = Math.max(2, Math.min(screenHeight - 10, cy + (cy > screenHeight / 2 ? -14 : 10)));
        graphics.drawString(font, Component.literal(label), textX, textY, color, true);
    }
}
