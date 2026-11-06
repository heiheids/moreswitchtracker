package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Draws a reticle on a highlighted player, through walls, with the distance above it.
 *
 * <p>The mark follows the player's <em>entity</em> while it is in the client's sync range. Past
 * that the entity no longer exists on this side, so the last position the server pushed is used
 * instead -- that is what keeps a far highlight visible at all.</p>
 */
public final class PlayerHighlightRenderer {

    /** Magenta, distinct from the purple death-point mark. */
    private static final int MARK_COLOR = 0xFFFF7BD0;
    private static final int TEXT_COLOR = 0xFFFFD6F2;
    private static final double SCREEN_FACTOR = 0.035;
    /** Closer than this the mark hugs the player; farther it is drawn along the view direction. */
    private static final double NEAR_LIMIT = 24.0;
    /** Distance from the eye at which far marks are drawn -- close enough to never hit the far plane. */
    private static final double ANCHOR_DISTANCE = 12.0;

    private static boolean warned;

    private PlayerHighlightRenderer() {
    }

    public static void register() {
        TrackerClientEvents.onGizmos(() -> {
            if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING)) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer self = minecraft.player;
            if (level == null || self == null || PlayerHighlightState.active().isEmpty()) {
                return;
            }
            try {
                Vec3 eye = self.getEyePosition();
                Vec3 view = self.getViewVector(1.0f);
                String dimension = level.dimension().identifier().toString();

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
                    // outside the camera's near/far planes or get culled, which is precisely why
                    // a highlight at 10000 blocks used to vanish. Both branches give the same
                    // apparent size, so the switch over is invisible.
                    Vec3 drawAt;
                    if (distance <= NEAR_LIMIT) {
                        drawAt = target;
                    } else {
                        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind the player: nothing sensible to draw yet
                        }
                        drawAt = eye.add(direction.scale(ANCHOR_DISTANCE));
                    }

                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, SCREEN_FACTOR);
                    TargetIcon.draw(drawAt, radius, eye, MARK_COLOR);

                    String caption = entry.name()
                            + "  " + Component.translatable("tracker.players.distance",
                                    String.format("%.0f", distance)).getString()
                            + "  " + formatTime(entry.remaining());
                    Gizmos.billboardText(caption, drawAt.add(0.0, radius * 1.9, 0.0),
                                    TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.9f))
                            .setAlwaysOnTop();
                }
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] player highlight could not draw; disabling it for this session", t);
                }
            }
        });
    }

    /**
     * Where to draw the mark: the live entity when it exists, otherwise the server's last known
     * position. A position from another dimension is ignored.
     */
    /** 12000 ticks -> "10:00". */
    static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
