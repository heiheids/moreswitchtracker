package com.heishandashu.tracker.module.mobglow;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * 怪物透视 / Mob Glow -- every hostile mob within 128 blocks gets the spectral-arrow outline.
 *
 * <p>This uses the game's own glowing flag ({@code Entity#setGlowingTag(boolean)}) rather than a
 * custom renderer, so the effect is byte-for-byte the vanilla spectral arrow outline: it renders
 * through walls, it uses the real entity silhouette, and it needs no per-frame drawing. That is
 * also why there is no client-side half of this module at all -- the flag is synced by vanilla.</p>
 *
 * <p>Each mob keeps the glow for {@link #HIGHLIGHT_TICKS} (2 minutes) counted from the moment it
 * was first lit. Standing inside the radius therefore keeps everything outlined continuously,
 * because a mob that is still in range is re-lit as its window expires. Walking away does not
 * cancel the remaining time -- that is the point of a timed highlight.</p>
 *
 * <p>Server-authoritative because the flag lives on the server entity and is sent to tracking
 * clients; a client-side flip would desync and be invisible to other players.</p>
 */
public final class MobGlowTrackingModule implements TrackerModule {

    /** Radius in blocks, measured from the player to the mob. */
    public static final double RADIUS = 128.0;

    /** How long one application lasts: 2 minutes at 20 tps. */
    public static final int HIGHLIGHT_TICKS = 2400;

    /** Re-scan every 10 ticks; the flag is cheap but there is no reason to do it every tick. */
    public static final int SCAN_INTERVAL = 10;

    /** Read on both sides, so flipping the switch takes effect immediately. */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.MOB_GLOW_TRACKING);
    }

    @Override
    public String id() {
        return ModuleIds.MOB_GLOW_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public void registerServer() {
        MobGlowTracker.init();
        // A dedicated server can be stopped while mobs are lit; clearing on stop keeps the
        // next world load from inheriting stale flags.
        ServerLifecycleEvents.SERVER_STOPPING.register(MobGlowTracker::clearAll);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            // Turning the switch off must actually turn the outlines off -- the glow flag has no
            // duration of its own, so nothing else would ever clear it. Passing null makes the
            // tracker use the server it last ticked on.
            MobGlowTracker.clearAll(null);
        }
    }
}

