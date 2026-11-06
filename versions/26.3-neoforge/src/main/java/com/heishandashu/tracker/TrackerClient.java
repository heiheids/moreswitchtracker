package com.heishandashu.tracker;

import com.heishandashu.moreswitches.client.MoreSwitchesClient;
import com.heishandashu.moreswitches.client.keybind.SettingsKeybinds;
import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.heishandashu.tracker.client.command.TrackerClientCommands;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import com.heishandashu.tracker.client.module.ClientTrackerModules;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client half of the NeoForge entrypoint -- only loaded on the physical client.
 *
 * <p>Starts the Tracker client modules first, then the More Switches half, and exposes the one
 * shared tabbed screen both halves live in. The old Tracker K binding is not registered: both
 * former screens are reachable through the single J binding.</p>
 */
public final class TrackerClient {

    private TrackerClient() {
    }

    public static void initialize(IEventBus modBus, ModContainer container) {
        ClientTrackerModules.registerAll();
        ClientTrackerModules.initializeClient();

        // Registration-time events first, so the pending HUD layers / tick actions registered by
        // the More Switches half below are drained by the bus listeners that already exist.
        TrackerClientEvents.registerBuses(modBus);
        modBus.addListener(SettingsKeybinds::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(TrackerClientCommands::registerCommands);

        MoreSwitchesClient.initializeClient();

        // The "config" button in the mods list opens the same screen.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ModContainer mod, Screen parent) -> new SettingsScreen(parent));

        TrackerMod.LOGGER.info("[{}] client ready -- three tabs, default key: J", TrackerMod.MOD_ID);
    }
}
