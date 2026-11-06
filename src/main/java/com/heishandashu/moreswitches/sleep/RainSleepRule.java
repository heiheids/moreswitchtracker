package com.heishandashu.moreswitches.sleep;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;

/**
 * Decides whether rain makes a bed usable in daylight.
 *
 * <p>Vanilla only lets a player sleep when the sky is dark, so a rainy midday cannot be slept
 * through even though it looks like night. With the switch on, rain counts as "dark enough", the
 * bed can be used, and vanilla then advances the clock to its own wake-up marker — the same skip a
 * night's sleep performs.</p>
 *
 * <p>The decision itself is a pure function over three booleans, so it is testable without a
 * running game; {@link #allowsSleep(Level)} is the one-line adapter that reads those booleans off
 * the world.</p>
 */
public final class RainSleepRule {

    private RainSleepRule() {
    }

    /**
     * Should a "sleep only when dark" rule be treated as satisfied right now?
     *
     * <p>All three conditions matter: the switch must be on, the dimension must have weather at
     * all (the Nether and the End do not, so they keep vanilla behaviour), and it must actually be
     * raining.</p>
     */
    public static boolean allowsSleep(Level level) {
        return allowsSleep(isActive(), level.isRaining(), hasWeather(level));
    }

    /**
     * Whether this dimension has weather.
     *
     * <p>Minecraft 1.21.1 has no {@code canHaveWeather()} on the level, so it is derived from the
     * dimension's biome tag: only dimensions with a biomes tag that allows precipitation can rain.
     * The Nether and the End are excluded by that tag, which is exactly the behaviour wanted.</p>
     */
    public static boolean hasWeather(Level level) {
        if (level.dimensionType().hasSkyLight()) {
            return !level.getBiome(level.getSharedSpawnPos()).is(BiomeTags.IS_NETHER);
        }
        return false;
    }

    /**
     * The rule itself.
     *
     * @param switchOn         whether the player turned the switch on
     * @param raining          whether rain is falling
     * @param weatherDimension whether this dimension has weather at all
     */
    public static boolean allowsSleep(boolean switchOn, boolean raining, boolean weatherDimension) {
        return switchOn && weatherDimension && raining;
    }

    /** Whether the player turned the switch on. Separate so a refusal can be explained. */
    public static boolean isActive() {
        return ModSettings.get().isEnabled(SwitchIds.RAIN_SLEEP);
    }
}
