package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.config.ModSettings;

/**
 * One switchable behaviour on the client.
 *
 * <p>A module is created once at startup; {@link #register(String)} binds it to its switch id and
 * installs any listeners it needs. After that the module only ever reacts to its own switch, so
 * two features cannot interfere with each other.</p>
 */
public interface ClientFeature {

    /**
     * Binds this module to the given switch id and starts observing the switch.
     *
     * @param switchId one of {@code SwitchIds}; the module is off from the start when the
     *                 settings file says so
     */
    void register(String switchId);

    /** Called whenever the player flips the switch, so side effects happen immediately. */
    void onEnabledChanged(boolean enabled);

    /** Convenience for subclasses: current state of the bound switch. */
    default boolean enabled(String switchId) {
        return ModSettings.get().isEnabled(switchId);
    }
}
