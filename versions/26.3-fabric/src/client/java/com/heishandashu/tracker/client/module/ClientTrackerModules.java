package com.heishandashu.tracker.client.module;

import com.heishandashu.tracker.module.TrackerModules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side companion of {@link TrackerModules}.
 *
 * <p>It mirrors the server registry one-to-one: same ids, same order. The client only keeps
 * the register/enable plumbing here and delegates all real work to the individual client
 * modules.</p>
 */
public final class ClientTrackerModules {

    private static final Map<String, ClientTrackerModule> BY_ID = new LinkedHashMap<>();
    private static final List<ClientTrackerModule> ORDERED = new ArrayList<>();
    private static boolean registered;

    private ClientTrackerModules() {
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        register(new com.heishandashu.tracker.client.module.legacy.LegacyTrackingClient());
        register(new com.heishandashu.tracker.client.module.players.PlayerTrackingClient());
        register(new com.heishandashu.tracker.client.module.traders.TraderTrackingClient());
        register(new com.heishandashu.tracker.client.module.persistent.PersistentMobTrackingClient());
        register(new com.heishandashu.tracker.client.module.lag.LagSourceTrackingClient());
        register(new com.heishandashu.tracker.client.module.elite.EliteMobTrackingClient());
        register(new com.heishandashu.tracker.client.module.gateway.EndGatewayTrackingClient());
        register(new com.heishandashu.tracker.client.module.relations.RelationTrackingClient());
        register(new com.heishandashu.tracker.client.module.coords.CustomCoordTrackingClient());
    }

    private static void register(ClientTrackerModule module) {
        if (BY_ID.putIfAbsent(module.id(), module) != null) {
            throw new IllegalStateException("duplicate tracker client module id: " + module.id());
        }
        ORDERED.add(module);
    }

    public static List<ClientTrackerModule> all() {
        return Collections.unmodifiableList(ORDERED);
    }

    public static void initializeClient() {
        for (ClientTrackerModule module : ORDERED) {
            module.registerClient();
        }
        for (ClientTrackerModule module : ORDERED) {
            module.onEnabledChanged(TrackerModules.isEnabled(module.id()));
        }
    }

    /** Called by the settings screen after a switch is flipped. */
    public static void notifyEnabledChanged(String id, boolean enabled) {
        ClientTrackerModule module = BY_ID.get(id);
        if (module != null) {
            module.onEnabledChanged(enabled);
        }
    }
}
