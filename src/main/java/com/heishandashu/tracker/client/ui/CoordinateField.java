package com.heishandashu.tracker.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * The coordinate field that sits left of the 自定义坐标追踪 switch.
 *
 * <p>Plain {@link EditBox} with two behaviours the settings screen needs:</p>
 * <ul>
 *   <li>it reports its text on <b>every</b> change, so the coordinate is saved as it is typed
 *       rather than on some later "apply" step that the player would have to discover;</li>
 *   <li>it can be re-filled from the saved value without that write-back echoing into the
 *       callback, which would fight the player's caret while they type.</li>
 * </ul>
 *
 * <p>Kept separate from the screen so the layout code stays about layout.</p>
 */
public final class CoordinateField {

    /** Long enough for three doubles and two spaces; short enough to stay one line. */
    private static final int MAX_LENGTH = 64;

    private final EditBox box;
    private final Consumer<String> onChanged;

    /** Guards {@link #setTextQuietly} so programmatic updates do not look like typing. */
    private boolean suppressCallback;

    public CoordinateField(Font font, int width, Component title, Component hint,
                           Consumer<String> onChanged) {
        this.onChanged = onChanged;
        this.box = new EditBox(font, 0, 0, width, 18, title);
        this.box.setMaxLength(MAX_LENGTH);
        this.box.setHint(hint);
        this.box.setResponder(this::handleChanged);
    }

    /** The widget to add to the screen. */
    public EditBox widget() {
        return this.box;
    }

    public String text() {
        return this.box.getValue();
    }

    /** Fills the field from stored state without firing the change callback. */
    public void setTextQuietly(String text) {
        if (text == null || text.equals(this.box.getValue())) {
            return;
        }
        this.suppressCallback = true;
        try {
            this.box.setValue(text);
        } finally {
            this.suppressCallback = false;
        }
    }

    private void handleChanged(String text) {
        if (this.suppressCallback) {
            return;
        }
        this.onChanged.accept(text);
    }
}
