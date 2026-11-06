package com.heishandashu.tracker.client.module.gateway;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Marks the end gateway 折跃门追踪器 found. */
public final class EndGatewayOverlay {

    private static final float RED = 0.62f;
    private static final float GREEN = 0.44f;
    private static final float BLUE = 1.0f;
    private static final int HUD_COLOR = 0xFFC98BFF;
    private static final double BOX_HALF = 1.0;

    private EndGatewayOverlay() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            EndGatewayTrackingClient.Marked mark = current();
            if (mark == null) {
                return;
            }
            PointHighlight.world(pose, buffers, new Vec3(mark.x(), mark.y(), mark.z()),
                    RED, GREEN, BLUE, 1.0f, BOX_HALF);
        });

        ClientEvents.addHudPass((graphics, partialTick) -> {
            EndGatewayTrackingClient.Marked mark = current();
            if (mark == null) {
                return;
            }
            Vec3 target = new Vec3(mark.x(), mark.y(), mark.z());
            double distance = Minecraft.getInstance().player == null
                    ? 0.0 : Minecraft.getInstance().player.getEyePosition().distanceTo(target);
            PointHighlight.hud(graphics, target,
                    PointHighlight.coords(mark.x(), mark.y(), mark.z()) + "  "
                            + PointHighlight.distanceText(distance) + "  "
                            + formatRemaining(mark), HUD_COLOR);
        });
    }

    private static EndGatewayTrackingClient.Marked current() {
        if (!TrackerModules.isEnabled(ModuleIds.END_GATEWAY_TRACKING)) {
            return null;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        EndGatewayTrackingClient.Marked mark =
                EndGatewayTrackingClient.marked(EndGatewayTrackingClient.now());
        if (mark == null || !mark.dimension().equals(level.dimension().location().toString())) {
            return null;
        }
        return mark;
    }

    private static String formatRemaining(EndGatewayTrackingClient.Marked mark) {
        int ticks = mark.remaining(EndGatewayTrackingClient.now());
        int seconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
