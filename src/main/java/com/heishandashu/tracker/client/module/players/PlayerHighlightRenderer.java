package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a reticle on a highlighted player, through walls, with the distance above it.
 *
 * <p>The mark follows the player's <em>entity</em> while it is in the client's sync range. Past
 * that the entity no longer exists on this side, so the last position the server pushed is used
 * instead -- that is what keeps a far highlight visible at all.</p>
 */
public final class PlayerHighlightRenderer {

    /** Magenta, distinct from the purple death-point mark. */
    private static final float RED = 1.0f;
    private static final float GREEN = 0.48f;
    private static final float BLUE = 0.82f;
    private static final int HUD_COLOR = 0xFFFF7BD0;

    private PlayerHighlightRenderer() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer self = minecraft.player;
            if (level == null || self == null || PlayerHighlightState.active().isEmpty()) {
                return;
            }
            Vec3 eye = self.getEyePosition();
            Vec3 view = self.getViewVector(1.0f);
            String dimension = level.dimension().location().toString();

            WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
            try {
                for (PlayerHighlightState.Entry entry : PlayerHighlightState.active()) {
                    Vec3 target = PlayerPositions.resolve(level, dimension, entry.uuid());
                    if (target == null) {
                        continue;
                    }
                    double distance = eye.distanceTo(target);
                    if (distance < 0.05) {
                        continue;
                    }

                    // Far away the marker is placed along the view direction at a fixed short
                    // distance: same screen position as the real target, but it can never fall
                    // outside the camera's near/far planes or get culled.
                    Vec3 drawAt;
                    if (distance <= PointHighlight.NEAR_LIMIT) {
                        drawAt = target;
                    } else {
                        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind the player: nothing sensible to draw yet
                        }
                        drawAt = eye.add(direction.scale(PointHighlight.ANCHOR_DISTANCE));
                    }

                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, PointHighlight.SCREEN_FACTOR);
                    TargetIcon.draw(batch, drawAt, radius, eye, RED, GREEN, BLUE, 1.0f);
                }
            } finally {
                batch.close();
            }
        });

        ClientEvents.addHudPass((graphics, partialTick) -> {
            if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer self = minecraft.player;
            if (level == null || self == null || PlayerHighlightState.active().isEmpty()) {
                return;
            }
            Vec3 eye = self.getEyePosition();
            String dimension = level.dimension().location().toString();

            for (PlayerHighlightState.Entry entry : PlayerHighlightState.active()) {
                Vec3 target = PlayerPositions.resolve(level, dimension, entry.uuid());
                if (target == null) {
                    continue;
                }
                double distance = eye.distanceTo(target);
                String caption = entry.name()
                        + "  " + Component.translatable("tracker.players.distance",
                                String.format("%.0f", distance)).getString()
                        + "  " + formatTime(entry.remaining());
                PointHighlight.hud(graphics, target, caption, HUD_COLOR);
            }
        });
    }

    /** 12000 ticks -> "10:00". */
    static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
