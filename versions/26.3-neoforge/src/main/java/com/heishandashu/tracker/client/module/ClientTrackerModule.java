package com.heishandashu.tracker.client.module;

import com.heishandashu.tracker.module.TrackerModule;

/**
 * Client half of a feature module.
 *
 * <p>Anything that touches screens, rendering, HUD or key state lives in this package tree. On
 * the Fabric side that tree was a separate {@code src/client} source set; NeoForge uses a single
 * source set, so these classes sit in {@code src/main} and are never loaded on a dedicated server
 * (the client bootstrap only runs behind the {@code Dist.CLIENT} check, and every client-only
 * helper keeps its loader types to itself). A client module is paired with a {@link TrackerModule}
 * that has the same {@link #id()}, and it is only ever active while that module's switch is
 * on.</p>
 */
public interface ClientTrackerModule {

    /** Must match the id of the paired {@link TrackerModule}. */
    String id();

    /**
     * Called once while the client initialises, regardless of the switch state. Register
     * render callbacks here and check the switch inside them.
     */
    default void registerClient() {
    }

    /** Called on startup and whenever the matching switch is flipped. Must be idempotent. */
    default void onEnabledChanged(boolean enabled) {
    }
}
