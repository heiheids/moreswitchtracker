package com.heishandashu.moreswitches.fullscreen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the full-screen-key decision.
 *
 * <p>These call {@link FullscreenKeyRule#blocksToggle(boolean)} — the same function the mixin
 * reaches, with the settings value supplied directly.</p>
 */
class FullscreenKeyRuleTest {

    @Test
    void withTheSwitchOnTheKeyIsIgnored() {
        assertTrue(FullscreenKeyRule.blocksToggle(true),
                "the full-screen key must do nothing while the switch is on");
    }

    @Test
    void withTheSwitchOffTheKeyWorksAsUsual() {
        assertFalse(FullscreenKeyRule.blocksToggle(false),
                "the full-screen key must behave exactly as vanilla while the switch is off");
    }
}
