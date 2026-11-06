package com.heishandashu.tracker;

import com.heishandashu.moreswitches.client.MoreSwitchesClient;
import com.heishandashu.tracker.client.command.TrackerClientCommands;
import com.heishandashu.tracker.client.module.ClientTrackerModules;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint -- runs on the physical client only.
 *
 * <p>Starts the tracker page first, then the switches page. There is only one
 * key binding now: {@code SettingsKeybinds} opens the tabbed screen on J.</p>
 */
public class TrackerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTrackerModules.registerAll();
        ClientTrackerModules.initializeClient();

        TrackerClientCommands.register();
        MoreSwitchesClient.initializeClient();

        TrackerMod.LOGGER.info("[{}] client ready -- three tabs, default key: J",
                TrackerMod.MOD_ID);
    }
}
