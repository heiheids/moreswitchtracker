package com.heishandashu.moreswitches.light;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * The rules behind the two light switches, with no Minecraft types in them.
 *
 * <p>The switches answer three questions: is the preview on, is the moving light on, and where
 * would a held light source actually be placed? Keeping the answers here means the mixins stay
 * thin adapters and the interesting part is testable.</p>
 */
public final class LightPreviewRule {

    private LightPreviewRule() {
    }

    /** Whether the numbered light-level overlay should be drawn. */
    public static boolean previewEnabled() {
        return ModSettings.get().isEnabled(SwitchIds.LIGHT_PREVIEW);
    }

    /** Whether the held light source should visually brighten its surroundings. */
    public static boolean movingLightEnabled() {
        return ModSettings.get().isEnabled(SwitchIds.MOVING_LIGHT);
    }

    /** Whether either light switch is on, so the per-block light path can be left alone when both are off. */
    public static boolean anyEnabled() {
        return previewEnabled() || movingLightEnabled();
    }

    /**
     * Whether a held block is a light source worth simulating.
     *
     * @param emission the light level the held block emits, 0 when it emits none
     */
    public static boolean isLightSource(int emission) {
        return emission > 0;
    }

    /**
     * How much light to add at a position, from a source of {@code emission} at {@code distance}.
     *
     * <p>Zero when the source is out of reach, which lets callers skip the work entirely for the
     * vast majority of blocks in the world.</p>
     */
    public static int addedLight(int emission, double distance) {
        if (!isLightSource(emission)) {
            return 0;
        }
        return LightFalloff.levelAt(emission, distance);
    }
}
