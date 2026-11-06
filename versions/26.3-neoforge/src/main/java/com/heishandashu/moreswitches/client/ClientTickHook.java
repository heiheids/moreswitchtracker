package com.heishandashu.moreswitches.client;

import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

/**
 * One client-tick door for every More Switches feature.
 *
 * <p>NeoForge's client tick event is registered by {@code TrackerClientEvents}; this class only
 * keeps the feature-facing helper, so no feature class needs to know about the loader.</p>
 */
public final class ClientTickHook {

    private ClientTickHook() {
    }

    public static void onEndTick(Consumer<Minecraft> listener) {
        TrackerClientEvents.onClientTick(() -> listener.accept(Minecraft.getInstance()));
    }
}
