package com.heishandashu.moreswitches.cooldown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Timing behaviour of the firework cooldown window.
 *
 * <p>These tests call {@link CooldownClock} directly with explicit timestamps; they never read the
 * real clock, so they cannot flake and they do not depend on a running game.</p>
 */
class CooldownClockTest {

    private static final long WINDOW = 3_000L;

    @Test
    void firstUseIsAllowedAndStartsTheWindow() {
        CooldownClock clock = new CooldownClock(WINDOW);

        assertFalse(clock.blockIfCooling(1_000L), "the first use must go through");
        assertTrue(clock.blockIfCooling(1_001L), "a use one millisecond later must be refused");
    }

    @Test
    void useIsAllowedAgainExactlyWhenTheWindowEnds() {
        CooldownClock clock = new CooldownClock(WINDOW);
        clock.blockIfCooling(0L);

        assertTrue(clock.blockIfCooling(2_999L), "still inside the window");
        assertFalse(clock.blockIfCooling(3_000L), "the window is over at exactly 3000 ms");
    }

    @Test
    void eachAllowedUseStartsANewWindow() {
        CooldownClock clock = new CooldownClock(WINDOW);
        assertFalse(clock.blockIfCooling(0L));
        assertFalse(clock.blockIfCooling(3_000L), "the second use must go through after the window");

        assertTrue(clock.blockIfCooling(5_999L), "the second use started its own three second window");
        assertFalse(clock.blockIfCooling(6_000L));
    }

    @Test
    void remainingSecondsRoundsUpAndReportsZeroWhenReady() {
        CooldownClock clock = new CooldownClock(WINDOW);
        clock.blockIfCooling(0L);

        assertEquals(3, clock.remainingSeconds(1L));
        assertEquals(2, clock.remainingSeconds(1_001L));
        assertEquals(1, clock.remainingSeconds(2_001L));
        assertEquals(1, clock.remainingSeconds(2_999L));
        assertEquals(0, clock.remainingSeconds(3_000L));
        assertEquals(0, clock.remainingSeconds(10_000L));
    }

    @Test
    void resetLetsTheNextUseThroughImmediately() {
        CooldownClock clock = new CooldownClock(WINDOW);
        clock.blockIfCooling(0L);
        assertTrue(clock.blockIfCooling(10L));

        clock.reset();

        assertFalse(clock.blockIfCooling(20L), "after a reset the switch must not keep blocking");
    }

    @Test
    void zeroWindowIsRejectedInsteadOfSilentlyNeverBlocking() {
        assertThrows(IllegalArgumentException.class, () -> new CooldownClock(0L));
        assertThrows(IllegalArgumentException.class, () -> new CooldownClock(-1L));
    }
}
