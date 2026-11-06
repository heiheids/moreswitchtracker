package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;

/**
 * Client half of 遗产追踪 / Legacy Tracking.
 *
 * <p>Receives the server's snapshot of tracked deaths, ticks the countdown, and hands drawing
 * to {@link LegacyGizmoRenderer} (through-wall highlight) and {@link LegacyHud} (countdown).
 * The payload receiver itself lives in {@code net/TrackerNetwork} on this version.</p>
 */
public final class LegacyTrackingClient implements ClientTrackerModule {

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.LEGACY_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        LegacyGizmoRenderer.register();
        LegacyHud.register();
        com.heishandashu.tracker.compat.ClientEvents.addTick(LegacyHighlightState::tick);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            LegacyHighlightState.hide();
        }
    }
}
