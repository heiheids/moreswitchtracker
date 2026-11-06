package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.DtpNetworking;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.resources.Identifier;

/**
 * Client half of 遗产追踪 / Legacy Tracking.
 *
 * <p>Receives the server's snapshot of tracked deaths, ticks the countdown, and hands drawing
 * to {@link LegacyGizmoRenderer} (through-wall highlight) and {@link LegacyHud} (countdown).</p>
 */
public final class LegacyTrackingClient implements ClientTrackerModule {

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "legacy_countdown");

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

        TrackerClientEvents.receive(DtpNetworking.LegacySyncPayload.TYPE, LegacyHighlightState::accept);

        TrackerClientEvents.onClientTick(LegacyHighlightState::tick);

        LegacyGizmoRenderer.register();
        TrackerClientEvents.onHudLayer(HUD_ID, LegacyHud::render);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            LegacyHighlightState.hide();
        }
    }
}
