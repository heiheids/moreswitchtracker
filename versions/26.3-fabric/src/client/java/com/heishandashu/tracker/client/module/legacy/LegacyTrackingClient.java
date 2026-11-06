package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.DtpNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
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

        ClientPlayNetworking.registerGlobalReceiver(DtpNetworking.LegacySyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> LegacyHighlightState.accept(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(client -> LegacyHighlightState.tick());

        LegacyGizmoRenderer.register();
        HudElementRegistry.addLast(HUD_ID, LegacyHud::render);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            LegacyHighlightState.hide();
        }
    }
}
