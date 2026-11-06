package com.heishandashu.tracker.client.module.elite;

import com.heishandashu.tracker.client.render.PointHighlight;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.module.elite.EliteMobTrackingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a marked elite mob: a red reticle in the world, its health while the player is close, and
 * a distance / countdown caption.
 *
 * <p>Ported from the 26.3 gizmo version. The reticle goes through {@link WorldPainter}; the two
 * floating captions became HUD labels, which on this version is the only way to draw text in
 * screen space attached to a world position.</p>
 */
public final class EliteMobOverlay {

    private static final float RED = 1.0f;
    private static final float GREEN = 0.42f;
    private static final float BLUE = 0.35f;
    private static final int HUD_COLOR = 0xFFFFD9D2;
    private static final int HEALTH_COLOR = 0xFFFFB4A6;

    private EliteMobOverlay() {
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
            Vec3 view = self.getViewVector(1.0f);

            WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
            try {
                for (EliteMobTrackingClient.Highlight elite : EliteMobTrackingClient.highlights()) {
                    if (!elite.dimension().equals(dimension)) {
                        continue;
                    }
                    Vec3 target = new Vec3(elite.x(), elite.y() + 2.4, elite.z());
                    double distance = eye.distanceTo(target);
                    if (distance < 0.05) {
                        continue;
                    }
                    Vec3 drawAt = target;
                    if (distance > PointHighlight.NEAR_LIMIT) {
                        Vec3 direction = target.subtract(eye).scale(1.0 / distance);
                        if (direction.dot(view) <= 0.05) {
                            continue; // behind: the HUD arrow covers it
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
            Vec3 eye = self.getEyePosition();

            for (EliteMobTrackingClient.Highlight elite : EliteMobTrackingClient.highlights()) {
                if (!elite.dimension().equals(dimension)) {
                    continue;
                }
                Vec3 target = new Vec3(elite.x(), elite.y() + 2.4, elite.z());
                double distance = eye.distanceTo(target);

                // Health while close enough to matter, on its own line above the name.
                if (distance <= EliteMobTrackingModule.OVERHEAD_RANGE) {
                    String health = Component.translatable("tracker.elite.health",
                            String.format("%.0f", elite.health()),
                            String.format("%.0f", elite.maxHealth())).getString();
                    com.heishandashu.tracker.client.render.WorldLabel.draw(
                            graphics, eye, self.getViewVector(1.0f),
                            Minecraft.getInstance().options.fov().get(),
                            target.add(0.0, 0.35, 0.0), health, HEALTH_COLOR);
                }

                String caption = elite.name() + "  " + PointHighlight.distanceText(distance)
                        + "  " + EliteMobTrackingClient.clock(elite.remaining());
                PointHighlight.hud(graphics, target, caption, HUD_COLOR);
            }
        });
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.ELITE_MOB_TRACKING);
    }
}
