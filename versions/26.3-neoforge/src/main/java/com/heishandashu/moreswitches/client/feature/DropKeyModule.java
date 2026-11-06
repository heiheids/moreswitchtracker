package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * The drop-key switch.
 *
 * <p>Its effect is a rule consulted by {@code MixinMultiPlayerGameMode}, which reads the setting
 * directly, so this module owns no per-frame work. It still exists because every switch must have
 * a module: a switch without one is a wiring error the settings screen would otherwise hide.</p>
 */
public final class DropKeyModule implements ClientFeature {

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.DROP_KEY_LOCK, this.active ? "on" : "off");
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.DROP_KEY_LOCK, enabled);
    }
}
