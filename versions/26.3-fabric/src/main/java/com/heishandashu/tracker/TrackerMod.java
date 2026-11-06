package com.heishandashu.tracker;

import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint -- runs on both the client and the dedicated server.
 *
 * <p>It only wires the module registry up. All feature behaviour lives in the individual
 * module packages; nothing about a specific feature belongs in this class.</p>
 */
public class TrackerMod implements ModInitializer {

    public static final String MOD_ID = "moreswitchtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TrackerModules.registerAll();

        // Writes any missing switch into config/tracker.json and notifies modules once.
        TrackerModules.initializeCommon();

        // Registers server-side event handlers. Handlers are registered even while a feature
        // is switched off; each handler checks its own switch, so flipping it takes effect
        // immediately without a restart.
        TrackerModules.initializeServer();

        StringBuilder enabled = new StringBuilder();
        for (TrackerModule module : TrackerModules.all()) {
            if (enabled.length() > 0) {
                enabled.append(", ");
            }
            enabled.append(module.id()).append('=').append(TrackerModules.isEnabled(module) ? "on" : "off");
        }
        LOGGER.info("[{}] {} modules ready: {}", MOD_ID, TrackerModules.all().size(), enabled);
    }
}
