package com.heishandashu.tracker;

import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.TrackerNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The one NeoForge entrypoint of the 更多开关追踪 build.
 *
 * <p>The tracking page keeps its module registry and its own client bootstrap; the switches page
 * keeps its feature registry and settings. This class starts both under the mod id
 * {@code moreswitchtracker}.</p>
 */
@Mod(TrackerMod.MOD_ID)
public final class TrackerMod {

    public static final String MOD_ID = "moreswitchtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TrackerMod(IEventBus modBus, ModContainer container) {
        TrackerModules.registerAll();

        // Writes any missing switch into config/tracker.json and notifies modules once.
        TrackerModules.initializeCommon();

        // Registers server-side event handlers. Handlers are registered even while a feature
        // is switched off; each handler checks its own switch, so flipping it takes effect
        // immediately without a restart.
        TrackerModules.initializeServer();

        // Payload channels are declared on the mod bus, once, after every module has queued its own.
        modBus.addListener(TrackerNetwork::register);

        // Client-only wiring. The dist check keeps every client class out of a dedicated
        // server's class loading; nothing below is touched when the dist is DEDICATED_SERVER.
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            TrackerClient.initialize(modBus, container);
        }

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
