package com.heishandashu.tracker.module.lag;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.LagNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * 实体卡顿源头追踪 / Entity Lag Source Tracking -- find what is actually costing tick time.
 *
 * <p>Measures the server's own tick work per entity, per ticking block entity and per chunk, then
 * reports the worst offenders with dimension, coordinates, name and count. It speaks up only when
 * the worst group really is expensive (see {@link LagProfiler}), so leaving the switch on does not
 * mean a chat message every five seconds.</p>
 *
 * <p>All measuring hooks live in {@code mixin/MixinServerLevel} and
 * {@code mixin/MixinBoundTickingBlockEntity}; the numbers are grouped by {@link LagAggregator}.
 * The switch is off by default because sampling is not free.</p>
 */
public final class LagSourceTrackingModule implements TrackerModule {

    @Override
    public String id() {
        return ModuleIds.LAG_SOURCE_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    /** Read on both sides, so flipping the switch takes effect immediately. */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.LAG_SOURCE_TRACKING);
    }

    @Override
    public void registerCommon() {
        PayloadTypeRegistry.clientboundPlay()
                .register(LagNetworking.LagListPayload.TYPE, LagNetworking.LagListPayload.STREAM_CODEC);
    }

    @Override
    public void registerServer() {
        LagProfiler.init();
    }
}
