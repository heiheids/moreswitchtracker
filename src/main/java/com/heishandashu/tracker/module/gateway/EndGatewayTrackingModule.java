package com.heishandashu.tracker.module.gateway;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;

/**
 * 折跃门追踪器 / End Gateway Tracker.
 *
 * <p>End gateways are the only way out to the outer islands, and missing the one you meant to take
 * costs a long walk. Turning this switch on while in the End finds the nearest gateway and marks it
 * for ten minutes; flipping the switch off and on again repeats the search, so it can be re-used
 * without waiting.</p>
 *
 * <p>The search runs on the client, over the chunks the client already has, and only when the
 * switch is flipped on -- nothing is scanned while it is off.</p>
 */
public final class EndGatewayTrackingModule implements TrackerModule {

    /** How long a gateway stays marked. */
    public static final int HIGHLIGHT_TICKS = 12000;

    /** Search radius in chunks (16 chunks = 256 blocks). */
    public static final int SEARCH_CHUNKS = 16;

    @Override
    public String id() {
        return ModuleIds.END_GATEWAY_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.END_GATEWAY_TRACKING);
    }
}
