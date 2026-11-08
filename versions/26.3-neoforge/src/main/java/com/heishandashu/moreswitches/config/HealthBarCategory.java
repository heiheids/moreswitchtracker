package com.heishandashu.moreswitches.config;

/**
 * The four kinds of creature the 血条 page can switch on separately.
 *
 * <p>Plain Java on purpose: {@link ModSettings} is loaded on a dedicated server as well, so this
 * must not reference any entity class.</p>
 */
public enum HealthBarCategory {

    /** Anything hostile: zombies, skeletons, creepers, slimes, pillagers, ... */
    MONSTER("monster"),

    /** Villagers and wandering traders. */
    VILLAGER("villager"),

    /** Iron, snow and copper golems. */
    GOLEM("golem"),

    /** Passive animals: cows, pigs, sheep, chickens, ... */
    ANIMAL("animal");

    private final String id;

    HealthBarCategory(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}
