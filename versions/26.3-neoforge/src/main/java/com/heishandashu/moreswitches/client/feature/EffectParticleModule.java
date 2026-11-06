package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * The status-effect-particle switch.
 *
 * <p>Its effect is a rule consulted by {@code MixinClientLevelParticles}, which reads the setting
 * directly, so this module owns no per-frame work. It exists because every switch must have a
 * module: a switch without one is a wiring error the settings screen would otherwise hide.</p>
 */
public final class EffectParticleModule implements ClientFeature {

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.EFFECT_PARTICLES_OFF, this.active ? "on" : "off");
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.EFFECT_PARTICLES_OFF, enabled);
    }
}
