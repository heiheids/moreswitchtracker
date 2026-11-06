package com.heishandashu.tracker;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.tracker.module.TrackerModules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 追踪页 · 1.21.1 NeoForge 公共初始化.
 *
 * <p>Plain bootstrap called by {@link MoreSwitchesMod}. Each page keeps its own startup order, so
 * the tracking page owns these three steps:</p>
 * <ol>
 *   <li>{@code registerAll()} -- fill the registry, so the config knows every switch id;</li>
 *   <li>{@code initializeCommon()} -- write missing defaults, then let each module hook its events;</li>
 *   <li>{@code initializeServer()} -- once a server actually exists, start the per-world parts.</li>
 * </ol>
 */
public final class TrackerMod {

    /** The mod id. Network ids and the tracker resource namespace use it too. */
    public static final String MOD_ID = MoreSwitchesMod.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger("MoreSwitchTracker/Tracker");

    private TrackerMod() {
    }

    /** Called once from the common entrypoint; safe to call twice. */
    public static void initialize(IEventBus modBus) {
        modBus.addListener(TrackerMod::commonSetup);
        // Server-side modules hook game-bus events (tick, death, entity join).
        NeoForge.EVENT_BUS.addListener(TrackerMod::serverStarted);
        LOGGER.info("[tracker] 1.21.1 NeoForge build loaded");
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TrackerModules.registerAll();
            TrackerModules.initializeCommon();
            LOGGER.info("[tracker] {} feature module(s) registered", TrackerModules.all().size());
        });
    }

    /**
     * Starts the per-world halves once a server is up.
     *
     * <p>Not in {@code FMLCommonSetupEvent}: the modules that watch a level need a running server,
     * and asking for one during setup is exactly the kind of early access that throws.</p>
     */
    public static void serverStarted(ServerStartedEvent event) {
        TrackerModules.initializeServer();
        LOGGER.info("[tracker] server side ready ({} module(s))", TrackerModules.all().size());
    }
}
