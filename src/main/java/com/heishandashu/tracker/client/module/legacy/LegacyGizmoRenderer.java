package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.client.render.EntityLookup;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.DtpNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Draws the death marker and every dropped item through walls.
 *
 * <p>26.3 used the vanilla gizmo API. 1.21.1 has none, so the same shapes go through
 * {@link WorldPainter} on a depth-independent line type, which reproduces the "always on top"
 * look. World-space text is not available either, so the floating captions became HUD labels
 * drawn by {@link LegacyHud}; the marker geometry is the part that must live in the world.</p>
 */
public final class LegacyGizmoRenderer {

    /** Purple, matching the mod icon's accent. */
    private static final float RED = 0.70f;
    private static final float GREEN = 0.41f;
    private static final float BLUE = 1.0f;
    private static final float ALPHA = 1.0f;

    /**
     * Icon half-size per block of distance: the world size grows with the distance, so the mark
     * keeps the same apparent size on screen.
     */
    private static final double ICON_SCREEN_FACTOR = 0.035;

    private LegacyGizmoRenderer() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            if (!TrackerModules.isEnabled(ModuleIds.LEGACY_TRACKING)) {
                return;
            }
            DtpNetworking.DeathSnap snap = LegacyHighlightState.active();
            if (snap == null || !LegacyHighlightState.isCurrentDimension(snap)) {
                return;
            }
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            draw(buffers, pose, snap);
        });
    }

    private static void draw(net.minecraft.client.renderer.MultiBufferSource buffers,
                             com.mojang.blaze3d.vertex.PoseStack pose,
                             DtpNetworking.DeathSnap snap) {
        Vec3 centre = new Vec3(snap.x() + 0.5, snap.y() + 0.5, snap.z() + 0.5);
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 eye = player == null ? centre.add(0.0, 0.0, 8.0) : player.getEyePosition();

        WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
        try {
            double radius = TargetIcon.screenConstantRadius(centre, eye, ICON_SCREEN_FACTOR);
            TargetIcon.draw(batch, centre, radius, eye, RED, GREEN, BLUE, ALPHA);

            // Every drop that is still there, found by the uuid the server sent.
            for (UUID uuid : snap.itemIds()) {
                Entity entity = EntityLookup.byUuid(uuid);
                if (!(entity instanceof ItemEntity item)) {
                    continue;
                }
                AABB box = item.getBoundingBox().inflate(0.12);
                batch.box(box, RED, GREEN, BLUE, ALPHA);
            }
        } finally {
            batch.close();
        }
    }

    /** 6000 ticks -> "5:00". */
    public static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
