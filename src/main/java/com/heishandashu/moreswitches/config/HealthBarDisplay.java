package com.heishandashu.moreswitches.config;

import java.util.Locale;

/**
 * What the 血条 page draws above a creature.
 *
 * <p>Stored in {@code moreswitches.json} as a short id, so a hand-edited file that says something
 * unknown falls back to {@link #OFF} instead of refusing to start. This enum is plain Java: the
 * settings class is also loaded on a dedicated server, where no client type may be touched.</p>
 */
public enum HealthBarDisplay {

    /** Nothing is drawn. */
    OFF("off"),

    /** The red bar only. */
    BAR("bar"),

    /** The current/maximum numbers only. */
    NUMBERS("numbers"),

    /** Bar and numbers together. */
    BOTH("both");

    private final String id;

    HealthBarDisplay(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public boolean showsBar() {
        return this == BAR || this == BOTH;
    }

    public boolean showsNumbers() {
        return this == NUMBERS || this == BOTH;
    }

    /** Parses a stored id; anything unrecognised means "off". */
    public static HealthBarDisplay byId(String raw) {
        if (raw != null) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            for (HealthBarDisplay candidate : values()) {
                if (candidate.id.equals(value)) {
                    return candidate;
                }
            }
        }
        return OFF;
    }

    /** The next value the settings button cycles to. */
    public HealthBarDisplay next() {
        return switch (this) {
            case OFF -> BAR;
            case BAR -> NUMBERS;
            case NUMBERS -> BOTH;
            case BOTH -> OFF;
        };
    }
}
