package com.heishandashu.moreswitches.cooldown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The end-to-end shape of the firework gate.
 *
 * <p>{@link CooldownClockTest} covers the timing arithmetic. This test covers the decision the
 * mixin actually asks for — "may this use go through?" — over the two facts the module combines:
 * whether the switch is on and whether the held item is a rocket. The naming says which way the
 * answer goes so a mis-wired switch cannot pass by accident.</p>
 */
class FireworkGateTest {

    private static final long WINDOW_MILLIS = 3_000L;

    @Test
    void firstRocketUseGoesThroughAndStartsTheWindow() {
        CooldownClock clock = new CooldownClock(WINDOW_MILLIS);
        assertFalse(blocked(true, true, clock, 1_000L),
                "the first use must reach the game so a rocket can actually be used");
        assertTrue(blocked(true, true, clock, 1_001L),
                "the second use one millisecond later must be refused");
    }

    @Test
    void everyOtherItemIsUntouched() {
        CooldownClock clock = new CooldownClock(WINDOW_MILLIS);
        assertFalse(blocked(true, false, clock, 1_000L), "a non-rocket item must never be blocked");
        assertFalse(blocked(true, false, clock, 1_001L), "a non-rocket item must never be blocked");
    }

    @Test
    void withTheSwitchOffRocketsAreNeverBlocked() {
        CooldownClock clock = new CooldownClock(WINDOW_MILLIS);
        assertFalse(blocked(false, true, clock, 1_000L), "switch off means no gate at all");
        assertFalse(blocked(false, true, clock, 1_001L), "switch off means no gate at all");
    }

    @Test
    void theGateReopensAfterTheWindow() {
        CooldownClock clock = new CooldownClock(WINDOW_MILLIS);
        blocked(true, true, clock, 1_000L);
        assertTrue(blocked(true, true, clock, 3_999L), "still inside the three second window");
        assertFalse(blocked(true, true, clock, 4_000L), "exactly three seconds later the gate reopens");
    }

    /**
     * The same decision the mixin performs: the switch must be on, the item must be a rocket, and
     * the clock must not still be counting down.
     */
    private static boolean blocked(boolean switchOn, boolean rocket, CooldownClock clock, long now) {
        if (!switchOn || !rocket) {
            return false;
        }
        return clock.blockIfCooling(now);
    }
}
