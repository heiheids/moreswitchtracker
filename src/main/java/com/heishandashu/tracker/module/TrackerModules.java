package com.heishandashu.tracker.module;

import com.heishandashu.tracker.config.TrackerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The module registry: the single place that knows every feature module.
 *
 * <p>The settings screen, the config file and the server/client startup all read the
 * feature list from here, so adding a module is a one-line change plus its own package.</p>
 */
public final class TrackerModules {

    private static final Map<String, TrackerModule> BY_ID = new LinkedHashMap<>();
    private static final List<TrackerModule> ORDERED = new ArrayList<>();
    private static boolean registered;

    private TrackerModules() {
    }

    /** Registers every feature module, in the order they should appear in the settings screen. */
    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        register(new com.heishandashu.tracker.module.legacy.LegacyTrackingModule());
        register(new com.heishandashu.tracker.module.players.PlayerTrackingModule());
        register(new com.heishandashu.tracker.module.traders.WanderingTraderTrackingModule());
        register(new com.heishandashu.tracker.module.persistent.PersistentMobTrackingModule());
        register(new com.heishandashu.tracker.module.lag.LagSourceTrackingModule());
        register(new com.heishandashu.tracker.module.elite.EliteMobTrackingModule());
        register(new com.heishandashu.tracker.module.gateway.EndGatewayTrackingModule());
        register(new com.heishandashu.tracker.module.relations.RelationModules.VillagerPoi());
        register(new com.heishandashu.tracker.module.relations.RelationModules.ZombieAggro());
        register(new com.heishandashu.tracker.module.relations.RelationModules.DragonAggro());
        register(new com.heishandashu.tracker.module.coords.CustomCoordTrackingModule());
        register(new com.heishandashu.tracker.module.mobglow.MobGlowTrackingModule());
    }

    public static void register(TrackerModule module) {
        TrackerModule previous = BY_ID.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalStateException("duplicate tracker module id: " + module.id());
        }
        ORDERED.add(module);
    }

    /** Immutable, display-ordered list of every module. */
    public static List<TrackerModule> all() {
        return Collections.unmodifiableList(ORDERED);
    }

    public static TrackerModule byId(String id) {
        return BY_ID.get(id);
    }

    public static boolean isEnabled(TrackerModule module) {
        return TrackerConfig.get().get(module.id(), module.defaultEnabled());
    }

    public static boolean isEnabled(String id) {
        TrackerModule module = BY_ID.get(id);
        return module != null && isEnabled(module);
    }

    /**
     * Flips one switch. Only the module that owns the switch is notified -- modules are
     * independent by design.
     *
     * @return the new state
     */
    public static boolean setEnabled(String id, boolean enabled) {
        TrackerModule module = BY_ID.get(id);
        if (module == null) {
            throw new IllegalArgumentException("unknown tracker module: " + id);
        }
        boolean changed = TrackerConfig.get().set(id, enabled);
        if (changed) {
            module.onEnabledChanged(enabled);
        }
        return enabled;
    }

    /** Called once from the common entrypoint: writes defaults and starts enabled modules. */
    public static void initializeCommon() {
        for (TrackerModule module : ORDERED) {
            TrackerConfig.get().ensureKnown(module.id(), module.defaultEnabled());
        }
        for (TrackerModule module : ORDERED) {
            module.registerCommon();
        }
        for (TrackerModule module : ORDERED) {
            module.onEnabledChanged(isEnabled(module));
        }
    }

    public static void initializeServer() {
        for (TrackerModule module : ORDERED) {
            module.registerServer();
        }
    }
}
