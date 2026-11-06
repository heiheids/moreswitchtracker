package com.heishandashu.moreswitches.drop;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * Decides whether the drop key must be ignored.
 *
 * <p>The decision is a pure function over one boolean, so it is testable without a running game;
 * {@link #blocksDrop()} is the one-line adapter that reads that boolean off the settings file.</p>
 */
public final class DropKeyRule {

    private DropKeyRule() {
    }

    /**
     * Should a drop request be swallowed?
     *
     * <p>Covers both the plain drop key and the drop-the-whole-stack variant: both go through the
     * same client method, and a player who turns the switch on expects neither to work.</p>
     */
    public static boolean blocksDrop() {
        return blocksDrop(isActive());
    }

    /**
     * The rule itself.
     *
     * @param switchOn whether the player turned the switch on
     */
    public static boolean blocksDrop(boolean switchOn) {
        return switchOn;
    }

    /** Whether the player turned the switch on. */
    public static boolean isActive() {
        return ModSettings.get().isEnabled(SwitchIds.DROP_KEY_LOCK);
    }
}
