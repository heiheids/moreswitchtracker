package com.heishandashu.moreswitches.effect;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * Decides whether status-effect particles should be drawn.
 *
 * <p>Kept as a pure function over one boolean so it is testable without a running client;
 * {@link #blocksEffectParticles()} is the adapter that reads that boolean off the settings file.</p>
 */
public final class EffectParticleRule {

    private EffectParticleRule() {
    }

    /**
     * Should a status-effect particle be dropped before it becomes a visible particle?
     *
     * <p>Only the particles are affected. The effect itself — its duration, its modifiers, the
     * hearts and hunger bar, the damage ticks — lives in the effect data and is never touched;
     * a player who turns this on keeps every gameplay effect and simply stops seeing the swirls.</p>
     */
    public static boolean blocksEffectParticles() {
        return blocksEffectParticles(isActive());
    }

    /**
     * The rule itself.
     *
     * @param switchOn whether the player turned the switch on
     */
    public static boolean blocksEffectParticles(boolean switchOn) {
        return switchOn;
    }

    /** Whether the player turned the switch on. */
    public static boolean isActive() {
        return ModSettings.get().isEnabled(SwitchIds.EFFECT_PARTICLES_OFF);
    }
}
