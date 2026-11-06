package com.heishandashu.tracker.module.persistent;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.PickedMobNetworking;
import com.heishandashu.tracker.net.TrackerNetwork;

/**
 * 不消失怪物追踪 / Persistence-Required Mob Tracking -- mobs that will never despawn.
 *
 * <p>Current focus: <b>mobs that picked up a dropped item</b>. Picking loot up makes vanilla call
 * {@code setPersistenceRequired()}, so those mobs stay in the world forever and keep loading
 * chunks. Each one is recorded as {@code #00001 生物名  物品名  维度+坐标}; short lists are shown
 * in chat, long ones go to a text file in the world save folder, and typing a {@code #id} in chat
 * highlights that mob for ten minutes.</p>
 *
 * <p>Server side lives here ({@link PickedMobTracker}, {@link PickedMobReport}); the highlight
 * lives in {@code src/client/java/.../client/module/persistent/}.</p>
 */
public final class PersistentMobTrackingModule implements TrackerModule {

    @Override
    public String id() {
        return ModuleIds.PERSISTENT_MOB_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    /** Read on both sides, so flipping the switch takes effect immediately. */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.PERSISTENT_MOB_TRACKING);
    }

    @Override
    public void registerCommon() {
        // Both sides must know the channel before anything is sent on it.
        TrackerNetwork.clientbound(PickedMobNetworking.PickedMobListPayload.TYPE,
                PickedMobNetworking.PickedMobListPayload.STREAM_CODEC);
    }

    @Override
    public void registerServer() {
        PickedMobTracker.init();
    }
}
