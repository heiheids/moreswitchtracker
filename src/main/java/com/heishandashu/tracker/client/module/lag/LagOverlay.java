package com.heishandashu.tracker.client.module.lag;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Marks a highlighted lag hot spot.
 *
 * <p>Same primitives as the other point features ({@link PointHighlight}), because a hot spot is
 * just a coordinate.</p>
 */
public final class LagOverlay {

    private static final float RED = 0.44f;
    private static final float GREEN = 0.85f;
    private static final float BLUE = 1.0f;
    private static final int HUD_COLOR = 0xFF6FD8FF;
    private static final double BOX_HALF = 0.5;

    private LagOverlay() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            for (LagState.Spot spot : visibleSpots()) {
                PointHighlight.world(pose, buffers, new Vec3(spot.x(), spot.y(), spot.z()),
                        RED, GREEN, BLUE, 1.0f, BOX_HALF);
            }
        });

        ClientEvents.addHudPass((graphics, partialTick) -> {
            for (LagState.Spot spot : visibleSpots()) {
                Vec3 target = new Vec3(spot.x(), spot.y(), spot.z());
                double distance = Minecraft.getInstance().player == null
                        ? 0.0 : Minecraft.getInstance().player.getEyePosition().distanceTo(target);
                PointHighlight.hud(graphics, target,
                        LagState.label(spot.id()) + "  " + spot.name() + "  "
                                + PointHighlight.distanceText(distance) + "  "
                                + LagState.clock(spot.remaining()),
                        HUD_COLOR);
            }
        });
    }

    private static java.util.List<LagState.Spot> visibleSpots() {
        if (!TrackerModules.isEnabled(ModuleIds.LAG_SOURCE_TRACKING)) {
            return java.util.List.of();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return java.util.List.of();
        }
        String dimension = level.dimension().location().toString();
        return LagState.highlights().stream()
                .filter(spot -> spot.dimension().equals(dimension))
                .toList();
    }
}
