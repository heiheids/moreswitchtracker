package com.heishandashu.tracker.client.module.coords;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.coords.CustomCoordTrackingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the custom coordinate: the same reticle the legacy death marker uses, plus a caption / edge
 * arrow.
 *
 * <p>Same behaviour as the 26.3 build, rewritten for the 1.21.1 rendering generation: the gizmo
 * calls became {@link PointHighlight}, and the floating text became a HUD label.</p>
 */
public final class CoordOverlay {

    /** Cyan-ish, distinct from the purple legacy mark and the pink player mark. */
    private static final float RED = 0.31f;
    private static final float GREEN = 0.82f;
    private static final float BLUE = 1.0f;
    private static final float ALPHA = 1.0f;
    private static final int HUD_COLOR = 0xFF4FD2FF;

    private CoordOverlay() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            CoordState.Point point = visiblePoint();
            if (point == null) {
                return;
            }
            PointHighlight.world(pose, buffers, position(point), RED, GREEN, BLUE, ALPHA, 0.0);
        });

        ClientEvents.addHudPass((graphics, partialTick) -> {
            CoordState.Point point = visiblePoint();
            if (point == null) {
                return;
            }
            Vec3 target = position(point);
            double distance = Minecraft.getInstance().player == null
                    ? 0.0 : Minecraft.getInstance().player.getEyePosition().distanceTo(target);
            PointHighlight.hud(graphics, target,
                    point.pretty() + "  " + PointHighlight.distanceText(distance), HUD_COLOR);
        });
    }

    /** The point to show, or null when the switch is off / nothing saved / wrong dimension. */
    private static CoordState.Point visiblePoint() {
        if (!CustomCoordTrackingModule.isActive()) {
            return null;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        CoordState.Point point = CoordState.point();
        if (point == null || !point.dimension().equals(level.dimension().location().toString())) {
            return null;
        }
        return point;
    }

    /**
     * Block coordinates are drawn at the centre of that block, exactly like the legacy death marker.
     *
     * <p>Drawing at the raw {@code x/y/z} would put the reticle on the block's corner and the old
     * box straddled four blocks, which read as "the marker is in the wrong place".</p>
     */
    private static Vec3 position(CoordState.Point point) {
        return new Vec3(Math.floor(point.x()) + 0.5,
                Math.floor(point.y()) + 0.5,
                Math.floor(point.z()) + 0.5);
    }
}
