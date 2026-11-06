package com.heishandashu.moreswitches.fullscreen;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * Decides whether the full-screen key must be ignored.
 *
 * <p>Expressed as a pure function over one boolean so it is testable without a running client;
 * {@link #blocksToggle()} is the adapter that reads that boolean off the settings file.</p>
 */
public final class FullscreenKeyRule {

    private FullscreenKeyRule() {
    }

    /**
     * Should the full-screen key be swallowed?
     *
     * <p>Only the key is affected. Full screen requested by other means — the video settings
     * button, a launch argument, the operating system — keeps working, because those paths do not
     * go through the key handler this rule guards.</p>
     */
    public static boolean blocksToggle() {
        return blocksToggle(isActive());
    }

    /**
     * The rule itself.
     *
     * @param switchOn whether the player turned the switch on
     */
    public static boolean blocksToggle(boolean switchOn) {
        return switchOn;
    }

    /** Whether the player turned the switch on. */
    public static boolean isActive() {
        return ModSettings.get().isEnabled(SwitchIds.FULLSCREEN_KEY_LOCK);
    }
}
