package com.heishandashu.tracker.module.traders;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.TraderNetworking;
import com.heishandashu.tracker.net.TrackerNetwork;

/**
 * 流浪商人追踪 / Wandering Trader Tracking (this replaces the former 村民追踪 switch).
 *
 * <p>When a wandering trader appears the player gets a chat message with its coordinates; clicking
 * them highlights the trader for ten minutes. The trader's own despawn countdown is shown above
 * its head whenever the player is within thirty blocks, whether or not the highlight is running,
 * so it is obvious how long there is to trade.</p>
 *
 * <p>Server side lives here ({@link TraderTracker}); the overlay and the highlight live in
 * {@code src/client/java/.../client/module/traders/}.</p>
 */
public final class WanderingTraderTrackingModule implements TrackerModule {

    /** How close the player has to be for the overhead countdown to appear. */
    public static final double OVERHEAD_RANGE = 30.0;

    @Override
    public String id() {
        return ModuleIds.WANDERING_TRADER_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    /** Read on both sides, so flipping the switch takes effect immediately. */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.WANDERING_TRADER_TRACKING);
    }

    @Override
    public void registerCommon() {
        TrackerNetwork.clientbound(TraderNetworking.TraderListPayload.TYPE,
                TraderNetworking.TraderListPayload.STREAM_CODEC);
    }

    @Override
    public void registerServer() {
        TraderTracker.init();
    }
}
