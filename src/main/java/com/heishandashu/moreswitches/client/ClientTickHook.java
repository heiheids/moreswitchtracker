package com.heishandashu.moreswitches.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The client tick hook, kept in one place.
 *
 * <p>NeoForge delivers a tick as an event on the game bus. Feature modules want to say "call me every
 * tick" without each of them learning the event API, so they register here and this class forwards
 * the call. It costs one indirection and keeps every module free of platform types.</p>
 */
public final class ClientTickHook {

    private static final List<Consumer<net.minecraft.client.Minecraft>> LISTENERS = new ArrayList<>();

    private static boolean registered;

    private ClientTickHook() {
    }

    /**
     * Adds a listener, called at the end of every client tick.
     *
     * <p>Safe to call during mod construction: the event itself is only subscribed once, the first
     * time a listener arrives.</p>
     */
    public static void onEndTick(Consumer<net.minecraft.client.Minecraft> listener) {
        LISTENERS.add(listener);
        if (!registered) {
            registered = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientTickHook.class);
        }
    }

    /** NeoForge entry point; not meant to be called by mod code. */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        // Copy first: a listener is allowed to register another one.
        for (Consumer<net.minecraft.client.Minecraft> listener : new ArrayList<>(LISTENERS)) {
            listener.accept(minecraft);
        }
    }
}
