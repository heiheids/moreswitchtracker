package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.client.render.EdgeArrow;
import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.traders.WanderingTraderTrackingModule;
import com.heishandashu.tracker.net.TraderNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the wandering trader: the despawn countdown above its head, and a reticle when the player
 * highlighted it.
 *
 * <p>The overhead countdown shows for any tracked trader within
 * {@link WanderingTraderTrackingModule#OVERHEAD_RANGE} blocks, whether or not a highlight is
 * running -- the trader vanishes on its own schedule, so the number has to be readable while
 * standing next to it.</p>
 *
 * <p>Ported from the 26.3 gizmo version: the reticle goes through {@link WorldPainter}, and the
 * two floating labels became HUD labels anchored on the trader.</p>
 */
public final class TraderOverlay {

    private static final int COUNTDOWN_COLOR = 0xFFFFE9A8;
    private static final int HUD_COLOR = 0xFFFFC24A;
    private static final float RED = 1.0f;
    private static final float GREEN = 0.76f;
    private static final float BLUE = 0.29f;

    private TraderOverlay() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            if (!enabled()) {
                return;
            }
            LocalPlayer self = Minecraft.getInstance().player;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || self == null) {
                return;
            }
            String dimension = level.dimension().location().toString();
            Vec3 eye = self.getEyePosition();

            WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
            try {
                for (TraderNetworking.TraderSnap snap : TraderState.traders()) {
                    if (!snap.dimension().equals(dimension) || !TraderState.isHighlighted(snap.uuid())) {
                        continue;
                    }
                    Vec3 where = TraderState.position(snap);
                    Vec3 drawAt = anchored(where, eye, self.getViewVector(1.0f));
                    if (drawAt == null) {
                        continue;
                    }
                    double radius = TargetIcon.screenConstantRadius(drawAt, eye, PointHighlight.SCREEN_FACTOR);
                    TargetIcon.draw(batch, drawAt, radius, eye, RED, GREEN, BLUE, 1.0f);
                }
            } finally {
                batch.close();
            }
        });

        ClientEvents.addHudPass((graphics, partialTick) -> {
            if (!enabled()) {
                return;
            }
            LocalPlayer self = Minecraft.getInstance().player;
            ClientLevel level = Minecraft.getInstance().level;
            if (self == null || level == null) {
                return;
            }
            String dimension = level.dimension().location().toString();

            for (TraderNetworking.TraderSnap snap : TraderState.traders()) {
                if (!snap.dimension().equals(dimension)) {
                    continue;
                }
                double distance = TraderState.distance(snap);
                Vec3 where = TraderState.position(snap);

                // 1) the despawn countdown, for any tracked trader the player is close to.
                if (distance <= WanderingTraderTrackingModule.OVERHEAD_RANGE) {
                    // A countdown of zero does not mean "about to vanish": it means this trader
                    // will never despawn (spawn egg, /summon, or a saved trader).
                    String countdown = snap.despawnTicks() > 0
                            ? Component.translatable("tracker.trader.countdown",
                                    TraderState.clock(snap.despawnTicks())).getString()
                            : Component.translatable("tracker.trader.never_despawns").getString();
                    com.heishandashu.tracker.client.render.WorldLabel.draw(
                            graphics, self.getEyePosition(), self.getViewVector(1.0f),
                            Minecraft.getInstance().options.fov().get(),
                            where.add(0.0, 0.9, 0.0), countdown, COUNTDOWN_COLOR);
                }

                // 2) only when highlighted: distance plus its own countdown, or an edge arrow.
                if (!TraderState.isHighlighted(snap.uuid())) {
                    continue;
                }
                String caption = Component.translatable("tracker.players.distance",
                        String.format("%.0f", distance)).getString()
                        + "  " + TraderState.clock(TraderState.highlightRemaining(snap.uuid()));
                PointHighlight.hud(graphics, where, caption, HUD_COLOR);
            }
        });
    }

    /**
     * Where to draw the reticle: the trader itself when close, otherwise a point along the view
     * direction so a far trader stays visible instead of being clipped by the far plane.
     */
    private static Vec3 anchored(Vec3 where, Vec3 eye, Vec3 view) {
        double distance = where.distanceTo(eye);
        if (distance <= PointHighlight.NEAR_LIMIT) {
            return where;
        }
        Vec3 direction = where.subtract(eye);
        if (direction.length() < 0.05) {
            return null;
        }
        direction = direction.scale(1.0 / direction.length());
        if (direction.dot(view) <= 0.05) {
            return null; // behind the player: nothing sensible to draw
        }
        return eye.add(direction.scale(PointHighlight.ANCHOR_DISTANCE));
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.WANDERING_TRADER_TRACKING);
    }
}
