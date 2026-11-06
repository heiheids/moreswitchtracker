package com.heishandashu.tracker;

import com.heishandashu.tracker.client.command.TrackerClientCommands;
import com.heishandashu.tracker.client.module.ClientTrackerModules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client initialisation of the Tracker half.
 *
 * <p>It is called from the client initialiser after the More Switches modules are up. It is
 * deliberately not a {@code @Mod}: one metadata entry means one mod id, and a second entrypoint
 * would never be loaded.</p>
 */
public final class TrackerClient {

    private TrackerClient() {
    }

    /** Called once from {@code MoreSwitchesClient}; idempotent so a second call cannot double-wire. */
    public static void initialise() {
        // Idempotent, and cheap insurance: the client registry asks the server registry whether
        // each switch is on, so it must be populated before initializeClient() runs. Common setup
        // normally did that already; calling it again is a no-op.
        com.heishandashu.tracker.module.TrackerModules.registerAll();
        com.heishandashu.tracker.module.TrackerModules.initializeCommon();

        ClientTrackerModules.registerAll();
        ClientTrackerModules.initializeClient();

        TrackerMod.LOGGER.info("[tracker] client ready -- {} feature module(s), shared settings key J",
                com.heishandashu.tracker.module.TrackerModules.all().size());
    }

    /** Client commands live on the game bus, next to the tick loop. */
    @EventBusSubscriber(modid = TrackerMod.MOD_ID, value = Dist.CLIENT)
    public static final class CommandBus {

        @SubscribeEvent
        public static void clientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(TrackerClientCommands.build());
        }
    }
}
