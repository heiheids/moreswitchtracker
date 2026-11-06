package com.heishandashu.tracker.client.module.persistent;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.PickedMobNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Marks a 不消失怪物 that the player asked to highlight.
 *
 * <p>Ported from the 26.3 gizmo version: the reticle goes through {@link WorldPainter}, and the
 * caption became a HUD label / edge arrow. The state keeps records and highlights separately, so
 * this joins them by UUID instead of assuming one object carries both.</p>
 */
public final class PickedMobHighlightRenderer {

    private static final float RED = 1.0f;
    private static final float GREEN = 0.76f;
    private static final float BLUE = 0.29f;
    private static final int HUD_COLOR = 0xFFFFE6B0;

    private PickedMobHighlightRenderer() {
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
                for (PickedMobHighlightState.Highlight highlight : PickedMobHighlightState.highlights()) {
                    PickedMobNetworking.PickedMobSnap snap =
                            PickedMobHighlightState.record(highlight.uuid());
                    if (snap == null || !snap.dimension().equals(dimension)) {
                        continue;
                    }
                    Vec3 target = PickedMobHighlightState.position(snap);
                    double distance = eye.distanceTo(target);
                    Vec3 drawAt = target;
                    if (distance > PointHighlight.NEAR_LIMIT) {
                        Vec3 direction = target.subtract(eye).scale(1.0 / Math.max(distance, 1.0E-4));
                        if (direction.dot(self.getViewVector(1.0f)) <= 0.05) {
                            continue; // behind the player
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
            if (!enabled()) {
                return;
            }
            LocalPlayer self = Minecraft.getInstance().player;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || self == null) {
                return;
            }
            String dimension = level.dimension().location().toString();
            for (PickedMobHighlightState.Highlight highlight : PickedMobHighlightState.highlights()) {
                PickedMobNetworking.PickedMobSnap snap =
                        PickedMobHighlightState.record(highlight.uuid());
                if (snap == null || !snap.dimension().equals(dimension)) {
                    continue;
                }
                Vec3 target = PickedMobHighlightState.position(snap);
                double distance = self.getEyePosition().distanceTo(target);
                String caption = "#" + String.format("%05d", highlight.id()) + "  "
                        + highlight.name() + "  " + PointHighlight.distanceText(distance)
                        + "  " + clock(highlight.remaining());
                PointHighlight.hud(graphics, target, caption, HUD_COLOR);
            }
        });
    }

    /** mm:ss for a tick countdown. */
    private static String clock(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.PERSISTENT_MOB_TRACKING);
    }
}
