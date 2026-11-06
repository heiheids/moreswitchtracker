package com.heishandashu.moreswitches.cooldown;

/**
 * The time window of the firework cooldown, with no Minecraft types in it.
 *
 * <p>Keeping the clock separate from "which item is this" makes the part that can actually be
 * wrong — the timing — testable without starting a game. {@link FireworkCooldownModule} owns the
 * item check and delegates every timing decision here.</p>
 *
 * <p>The window is measured on the wall clock, not in ticks: when the client lags, two clicks can
 * land inside one tick, and lag is exactly when a player double-clicks.</p>
 */
public final class CooldownClock {

    private final long windowMillis;

    /** Epoch millis until which use is refused; 0 means "ready". */
    private long blockedUntil;

    public CooldownClock(long windowMillis) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("cooldown window must be positive: " + windowMillis);
        }
        this.windowMillis = windowMillis;
    }

    /**
     * Asks whether a use may go through, and starts the window if it may.
     *
     * @param nowMillis current time, passed in so the caller (and tests) control the clock
     * @return true when the use must be refused because the window is still open
     */
    public boolean blockIfCooling(long nowMillis) {
        if (nowMillis < this.blockedUntil) {
            return true;
        }
        this.blockedUntil = nowMillis + this.windowMillis;
        return false;
    }

    /** Whole seconds left, rounded up; 0 when a use is allowed again. */
    public int remainingSeconds(long nowMillis) {
        long remaining = this.blockedUntil - nowMillis;
        if (remaining <= 0L) {
            return 0;
        }
        return (int) ((remaining + 999L) / 1000L);
    }

    /** Drops any running window; used when the player turns the switch off. */
    public void reset() {
        this.blockedUntil = 0L;
    }

    /** The configured window length. */
    public long windowMillis() {
        return this.windowMillis;
    }
}
