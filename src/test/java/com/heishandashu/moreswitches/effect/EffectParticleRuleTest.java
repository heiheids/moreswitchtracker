package com.heishandashu.moreswitches.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the status-effect-particle decision.
 *
 * <p>These call {@link EffectParticleRule#blocksEffectParticles(boolean)} — the same function the
 * mixin reaches, with the settings value supplied directly.</p>
 */
class EffectParticleRuleTest {

    @Test
    void withTheSwitchOnParticlesAreDropped() {
        assertTrue(EffectParticleRule.blocksEffectParticles(true),
                "the particle must be refused before it becomes visible");
    }

    @Test
    void withTheSwitchOffParticlesAreUntouched() {
        assertFalse(EffectParticleRule.blocksEffectParticles(false),
                "particles must behave exactly as vanilla while the switch is off");
    }
}
