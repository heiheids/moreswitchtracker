package com.heishandashu.moreswitches.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One Fabric client-tick door for every More Switches feature.
 *
 * <p>Replaces the NeoForge event-bus subscription of the 1.21.1 build. The listener is registered
 * once, on first use; features keep calling {@link #onEndTick(Consumer)} during client init.</p>
 */
public final class ClientTickHook {

    private static final List<Consumer<Minecraft>> LISTENERS = new ArrayList<>();
    private static boolean registered;

    private ClientTickHook() {
    }

    public static void onEndTick(Consumer<Minecraft> listener) {
        LISTENERS.add(listener);
        if (!registered) {
            registered = true;
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                for (Consumer<Minecraft> action : new ArrayList<>(LISTENERS)) {
                    action.accept(client);
                }
            });
        }
    }
}
