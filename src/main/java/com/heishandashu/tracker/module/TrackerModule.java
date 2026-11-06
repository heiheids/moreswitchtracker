package com.heishandashu.tracker.module;

/**
 * One feature switch = one module = one (or a few) self-contained classes.
 *
 * <p>Modules never talk to each other: each one owns its own listeners, its own state and
 * its own client-side part. Turning one off must never change the behaviour of another, so
 * a module must not read another module's state. Shared plumbing (config, i18n, screen
 * layout, network channel) lives outside the modules.</p>
 *
 * <p>Implementation notes for a new module:</p>
 * <ol>
 *   <li>add an id constant in {@link ModuleIds}</li>
 *   <li>create {@code module/<name>/<Name>Module.java} implementing this interface</li>
 *   <li>register it in {@link TrackerModules#registerAll()}</li>
 *   <li>add the name/description keys to {@code assets/tracker/lang/*.json}</li>
 *   <li>if it has client-only visuals, add a matching class under
 *       {@code src/client/java/.../client/module/<name>/}</li>
 * </ol>
 */
public interface TrackerModule {

    /** Stable id, must match {@link ModuleIds}. */
    String id();

    /** Value used the first time the feature is seen; written to the config file on load. */
    boolean defaultEnabled();

    /**
     * Called once on both sides while the mod initialises, regardless of the switch state.
     * Register commands and network payloads here -- things that must exist even when the
     * feature is currently off.
     */
    default void registerCommon() {
    }

    /**
     * Called once on the logical server (integrated server or dedicated server) while the
     * mod initialises, regardless of the switch state.
     */
    default void registerServer() {
    }

    /**
     * Called whenever the switch is flipped, including once at startup so a module can
     * start its own work if it is enabled. Implementations must be safe to call twice.
     */
    default void onEnabledChanged(boolean enabled) {
    }
}
