package com.heishandashu.tracker.module.elite;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;

/**
 * 精英怪追踪 / Elite Mob Tracking.
 *
 * <p>Some mobs have far more health than their peers -- a ravager, a warden, a wither, or anything
 * a command or another mod has buffed. This feature finds them and points them out, so a tough
 * fight is never a surprise.</p>
 *
 * <p>Detection happens on the client, which is where the entities around the player are already
 * known: no packet, no server hook, and nothing is measured while the switch is off. The tradeoff
 * is that only entities the client has loaded are found, which is exactly the area the player can
 * act on.</p>
 */
public final class EliteMobTrackingModule implements TrackerModule {

    /** A mob at or above this much health counts as elite. */
    public static final float ELITE_MIN_HEALTH = 100.0f;
    /** ...or this many times the usual health for its kind (catches buffed ordinary mobs). */
    public static final double ELITE_HEALTH_RATIO = 3.0;

    /** How close the player has to be for the health readout to appear above a marked mob. */
    public static final double OVERHEAD_RANGE = 30.0;

    @Override
    public String id() {
        return ModuleIds.ELITE_MOB_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.ELITE_MOB_TRACKING);
    }
}
