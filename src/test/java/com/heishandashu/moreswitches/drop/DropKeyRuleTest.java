package com.heishandashu.moreswitches.drop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the drop-key decision.
 *
 * <p>These call {@link DropKeyRule#blocksDrop(boolean)} — the same function the mixin reaches,
 * with the settings value supplied directly.</p>
 */
class DropKeyRuleTest {

    @Test
    void withTheSwitchOnEveryDropIsRefused() {
        assertTrue(DropKeyRule.blocksDrop(true),
                "the drop key must do nothing while the switch is on");
    }

    @Test
    void withTheSwitchOffDroppingWorksAsUsual() {
        assertFalse(DropKeyRule.blocksDrop(false),
                "the drop key must behave exactly as vanilla while the switch is off");
    }
}
