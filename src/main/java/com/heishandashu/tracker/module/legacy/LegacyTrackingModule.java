package com.heishandashu.tracker.module.legacy;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.DtpNetworking;

/**
 * 遗产追踪 / Legacy Tracking -- everything that happens after the player dies.
 *
 * <p>On death the player gets a clickable coordinate in chat; clicking it draws the death area
 * and every dropped item through walls for five minutes, with a countdown that pauses while
 * the chunk is unloaded. Drops that are destroyed, picked up or that expire are reported in
 * chat, and the ones that time out are listed at the end.</p>
 *
 * <p>Server side lives here; the highlight, HUD and commands live in
 * {@code src/client/java/.../client/module/legacy/}.</p>
 */
public final class LegacyTrackingModule implements TrackerModule {

    @Override
    public String id() {
        return ModuleIds.LEGACY_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    /**
     * Read by the tracker on every entry point, so flipping the switch takes effect at once
     * and no feature code keeps running behind it.
     */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.LEGACY_TRACKING);
    }

    @Override
    public void registerCommon() {
        // Channels are registered centrally in net/TrackerNetwork (NeoForge hands out one
        // registrar for the whole mod), so the module itself has nothing to register.
    }

    @Override
    public void registerServer() {
        LegacyTracker.init();
    }
}
