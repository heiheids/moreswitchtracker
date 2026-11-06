package com.heishandashu.tracker.client.module.players;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Draws the {@code @} suggestion list right above the chat input, the way vanilla draws command
 * suggestions.
 *
 * <p>The list is a popup over the chat screen, not a screen of its own: the player keeps typing in
 * the same box, so {@code @} still means whatever they want it to mean.</p>
 */
public final class MentionPopup {

    public static final int ROW_HEIGHT = 12;
    public static final int WIDTH = 140;

    private static final int BACKGROUND = 0xF0101010;
    private static final int BORDER = 0xFF3A3A3A;
    private static final int SELECTED_BACKGROUND = 0x40FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int SELECTED_TEXT_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFF909090;

    private MentionPopup() {
    }

    /** Rows that fit above the input; never more than {@link MentionSuggestions#MAX_ROWS}. */
    public static int visibleRows(EditBox input, int matchCount) {
        int fit = Math.max(1, (input.getY() - 4) / ROW_HEIGHT);
        return Math.min(Math.min(fit, MentionSuggestions.MAX_ROWS), matchCount);
    }

    public static int left(EditBox input) {
        return input.getX();
    }

    public static int top(EditBox input, int rows) {
        return input.getY() - rows * ROW_HEIGHT - 1;
    }

    /** Row index under the mouse, or -1. Two pixels of slack so a click is easy to land. */
    public static int rowAt(EditBox input, int rows, double mouseX, double mouseY) {
        int left = left(input);
        if (mouseX < left - 2 || mouseX > left + WIDTH + 2) {
            return -1;
        }
        int top = top(input, rows);
        if (mouseY < top - 2 || mouseY > top + rows * ROW_HEIGHT + 2) {
            return -1;
        }
        int row = (int) ((mouseY - top) / ROW_HEIGHT);
        if (row < 0) {
            row = 0;
        }
        return row >= rows ? rows - 1 : row;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, EditBox input,
                            List<MentionSuggestions.Suggestion> matches, int selected,
                            int mouseX, int mouseY) {
        int rows = visibleRows(input, matches.size());
        int left = left(input);
        int top = top(input, rows);

        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + rows * ROW_HEIGHT + 1, BACKGROUND);
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top, BORDER);

        int hovered = rowAt(input, rows, mouseX, mouseY);
        for (int row = 0; row < rows; row++) {
            int rowY = top + row * ROW_HEIGHT;
            boolean active = row == selected || row == hovered;
            if (active) {
                graphics.fill(left - 1, rowY, left + WIDTH + 1, rowY + ROW_HEIGHT, SELECTED_BACKGROUND);
            }
            graphics.text(font, Component.literal(matches.get(row).name()), left + 3, rowY + 2,
                    active ? SELECTED_TEXT_COLOR : TEXT_COLOR);
        }

        // Hint goes ABOVE the list: below it would sit on the chat input box itself.
        graphics.text(font, Component.translatable("tracker.players.suggest.hint"),
                left + 3, top - 10, HINT_COLOR);
    }
}
