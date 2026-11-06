package com.heishandashu.moreswitches.sleep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the rain-sleep decision.
 *
 * <p>These call {@link RainSleepRule#allowsSleep(boolean, boolean, boolean)} — the same function
 * the in-game path reaches, just with the world's three facts supplied directly.</p>
 */
class RainSleepRuleTest {

    @Test
    void withoutTheSwitchRainMustNotUnlockABed() {
        assertFalse(RainSleepRule.allowsSleep(false, true, true),
                "the vanilla restriction must stay in place while the switch is off");
    }

    @Test
    void withTheSwitchAndRainABedUnlocks() {
        assertTrue(RainSleepRule.allowsSleep(true, true, true),
                "rain while the switch is on must allow sleeping");
    }

    @Test
    void withTheSwitchButNoRainNothingChanges() {
        assertFalse(RainSleepRule.allowsSleep(true, false, true),
                "dry weather must leave the bed locked by daylight");
    }

    @Test
    void dimensionsWithoutWeatherAreNeverAffected() {
        assertFalse(RainSleepRule.allowsSleep(true, true, false),
                "the Nether and the End have no weather, so the switch must not touch them");
    }
}
