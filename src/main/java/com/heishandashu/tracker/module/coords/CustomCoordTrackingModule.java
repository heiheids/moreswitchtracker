package com.heishandashu.tracker.module.coords;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;

/**
 * 自定义坐标追踪 / Custom Coordinate Tracking.
 *
 * <p>The player types a coordinate and the mod highlights that spot until the switch is turned
 * off again. Unlike the other highlight features there is no server half at all: a coordinate is
 * a fixed point in the world, so the client can draw it by itself, and nothing needs to be
 * measured, scanned or sent.</p>
 *
 * <p>This class exists only so the switch appears in the settings screen and in
 * {@code config/tracker.json} like every other feature; the behaviour lives in
 * {@code src/client/java/.../client/module/coords/}.</p>
 */
public final class CustomCoordTrackingModule implements TrackerModule {

    /**
     * Config key holding the last coordinate the player entered, as
     * {@code <dimension>|<x>|<y>|<z>}.
     *
     * <p>It is deliberately persistent: the point of the feature is to come back to the same
     * place, so the value survives a restart and is never cleared on its own.</p>
     */
    public static final String SAVED_COORD_KEY = "custom_coord_tracking.point";

    /** Highlight markers are drawn this far along the view vector, so a far point stays visible. */
    public static final double ANCHOR_DISTANCE = 12.0;

    /** Inside this distance the marker hugs the real coordinate instead of the anchor. */
    public static final double NEAR_LIMIT = 24.0;

    @Override
    public String id() {
        return ModuleIds.CUSTOM_COORD_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.CUSTOM_COORD_TRACKING);
    }
}
