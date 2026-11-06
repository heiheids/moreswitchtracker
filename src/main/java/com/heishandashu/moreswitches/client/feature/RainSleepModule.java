package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * The rain-sleep switch.
 *
 * <p>Its effect is a rule consulted by {@code MixinBedRule}, which reads the setting directly, so
 * this module owns no per-frame work. It still exists for one reason: every switch must have a
 * module, and a switch without one is a programming error the settings screen would otherwise
 * hide. Logging the transition also makes the switch observable in a bug report.</p>
 */
public final class RainSleepModule implements ClientFeature {

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.RAIN_SLEEP, this.active ? "on" : "off");
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.RAIN_SLEEP, enabled);
    }
}
