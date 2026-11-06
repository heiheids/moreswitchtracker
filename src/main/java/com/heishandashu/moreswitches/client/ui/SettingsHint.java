package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.client.keybind.SettingsKeybinds;

/**
 * Turns the bound key into display text for the hint line.
 *
 * <p>Kept separate so {@link SettingsScreen} never has to know how a key binding is read, and
 * so the hint still reads sensibly when the binding was changed by the player.</p>
 */
final class SettingsHint {

    private SettingsHint() {
    }

    /** The currently bound key, or {@code unbound} when the player cleared it. */
    static String keyLabel() {
        if (SettingsKeybinds.OPEN_SETTINGS.isUnbound()) {
            return "?";
        }
        return SettingsKeybinds.OPEN_SETTINGS.getTranslatedKeyMessage().getString();
    }
}
