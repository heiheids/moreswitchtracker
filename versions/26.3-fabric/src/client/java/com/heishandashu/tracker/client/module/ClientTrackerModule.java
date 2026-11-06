package com.heishandashu.tracker.client.module;

import com.heishandashu.tracker.module.TrackerModule;

/**
 * Client half of a feature module.
 *
 * <p>The mod splits its sources into {@code main} (server + common) and {@code client}, so
 * anything that touches screens, rendering, HUD or key state lives here. A client module is
 * paired with a {@link TrackerModule} that has the same {@link #id()}, and it is only ever
 * active while that module's switch is on.</p>
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
