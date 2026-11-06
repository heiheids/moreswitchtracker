package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

/**
 * Ties the {@code @} mention list to the chat screen: keeps the current state, and turns keys and
 * clicks into a completion (or an immediate highlight).
 *
 * <p>Everything is derived from the chat box's own text every frame, so there is no mode to get
 * stuck in: delete the mention and the list disappears.</p>
 */
public final class MentionInput {

    // 26.3 runs on SDL, so the event constants are SDL's, not GLFW's: SDLK_TAB is 9, SDLK_RETURN
    // 13 and the arrow keys are 0x40000051/0x40000052, where GLFW would say 258/257/264/265.
    // Matching both sets (and both accessors) keeps this working whichever the platform reports.
    private static final int[] KEY_UP = {265, 0x40000052};
    private static final int[] KEY_DOWN = {264, 0x40000051};
    private static final int[] KEY_TAB = {258, 9};
    private static final int[] KEY_ENTER = {257, 13};
    private static final int[] KEY_ENTER_KEYPAD = {335, 0x40000058};

    private MentionInput() {
    }

    private static boolean matches(KeyEvent event, int[] codes) {
        int primary = event.key();
        int secondary = event.keycode();
        for (int code : codes) {
            if (primary == code || secondary == code) {
                return true;
            }
        }
        return false;
    }

    /** Current mention state, or null when there is nothing to suggest. */
    public record State(EditBox input, MentionSuggestions.Token token,
                        List<MentionSuggestions.Suggestion> matches, int selected) {
    }

    public static State state(Screen screen) {
        if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
            return null;
        }
        if (!(screen.getFocused() instanceof EditBox input)) {
            return null;
        }
        MentionSuggestions.Token token =
                MentionSuggestions.tokenAt(input.getValue(), input.getCursorPosition());
        if (token == null) {
            return null;
        }
        List<MentionSuggestions.Suggestion> matches = MentionSuggestions.matches(token.query());
        if (matches.isEmpty()) {
            return null;
        }
        MentionSuggestions.syncSelection(token.query(), matches.size());
        return new State(input, token, matches, MentionSuggestions.selected());
    }

    /** Called from the chat screen's render pass. */
    public static void draw(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        State state = state(screen);
        if (state == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        MentionPopup.draw(graphics, font, state.input(), state.matches(), state.selected(), mouseX, mouseY);
    }

    /** @return true when the key was used by the list and vanilla should not see it */
    public static boolean keyPressed(Screen screen, KeyEvent event) {
        State state = state(screen);
        if (state == null) {
            return false;
        }
        if (matches(event, KEY_DOWN)) {
            MentionSuggestions.move(1, state.matches().size());
            return true;
        }
        if (matches(event, KEY_UP)) {
            MentionSuggestions.move(-1, state.matches().size());
            return true;
        }
        // Tab walks the list and Enter takes the highlighted name -- both highlight straight
        // away, so picking someone never requires sending a chat message.
        if (matches(event, KEY_TAB)) {
            MentionSuggestions.move(1, state.matches().size());
            return true;
        }
        if (matches(event, KEY_ENTER) || matches(event, KEY_ENTER_KEYPAD)) {
            complete(state, state.matches().get(state.selected()), true);
            return true;
        }
        return false;
    }

    /** @return true when the click was used by the list */
    public static boolean mouseClicked(Screen screen, MouseButtonEvent event) {
        State state = state(screen);
        if (state == null) {
            return false;
        }
        // Any button picks: SDL numbers the left button 1, GLFW 0, and guessing wrong here made
        // clicks do nothing at all.
        int rows = MentionPopup.visibleRows(state.input(), state.matches().size());
        int row = MentionPopup.rowAt(state.input(), rows, event.x(), event.y());
        if (row < 0 || row >= state.matches().size()) {
            return false;
        }
        // Picking from the list highlights straight away -- no need to send anything.
        complete(state, state.matches().get(row), true);
        return true;
    }

    private static void complete(State state, MentionSuggestions.Suggestion suggestion, boolean highlight) {
        EditBox input = state.input();
        String completed = MentionSuggestions.completedText(input.getValue(), state.token(), suggestion);
        if (completed != null) {
            input.setValue(completed);
            input.setCursorPosition(MentionSuggestions.completedCursor(state.token(), suggestion));
        }
        if (highlight) {
            AtMentionHandler.highlightNow(suggestion.name());
        }
        MentionSuggestions.reset();
    }
}
