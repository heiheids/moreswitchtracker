package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;

/**
 * A switch whose effect is read straight from the settings file by a mixin.
 *
 * <p>Most of the mod's switches work this way: the mixin asks a rule class whether it may act, and
 * the rule reads the saved value. Such a switch still needs a module, because a switch without one
 * is a wiring error the settings screen would otherwise hide, but the module itself owns no state
 * beyond remembering what it last logged.</p>
 *
 * <p>This class exists so those switches do not each get a near-identical file.</p>
 */
public final class SimpleSwitchModule implements ClientFeature {

    private final String switchId;

    private boolean active;

    /**
     * @param switchId the switch this module stands for, used only for logging
     */
    public SimpleSwitchModule(String switchId) {
        this.switchId = switchId;
    }

    @Override
    public void register(String boundSwitchId) {
        this.active = enabled(boundSwitchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                this.switchId, this.active ? "on" : "off");
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, this.switchId, enabled);
    }
}
