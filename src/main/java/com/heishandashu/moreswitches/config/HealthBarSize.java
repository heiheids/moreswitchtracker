package com.heishandashu.moreswitches.config;

import java.util.Locale;

/**
 * Size of the overhead health bar and its numbers.
 *
 * <p>Only a factor is stored here; the pixel geometry lives in the client renderer, so this enum
 * stays loadable on a dedicated server.</p>
 */
public enum HealthBarSize {

    SMALL("small", 0.72F),
    MEDIUM("medium", 1.0F),
    LARGE("large", 1.36F);

    private final String id;
    private final float factor;

    HealthBarSize(String id, float factor) {
        this.id = id;
        this.factor = factor;
    }

    public String id() {
        return this.id;
    }

    /** Multiplier applied to the base bar geometry and to the text scale. */
    public float factor() {
        return this.factor;
    }

    public static HealthBarSize byId(String raw) {
        if (raw != null) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            for (HealthBarSize candidate : values()) {
                if (candidate.id.equals(value)) {
                    return candidate;
                }
            }
        }
        return MEDIUM;
    }

    public HealthBarSize next() {
        return switch (this) {
            case SMALL -> MEDIUM;
            case MEDIUM -> LARGE;
            case LARGE -> SMALL;
        };
    }
}
