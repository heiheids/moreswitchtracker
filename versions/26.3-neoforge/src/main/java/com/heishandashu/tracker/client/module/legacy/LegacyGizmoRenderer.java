package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.render.TargetIcon;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.DtpNetworking;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Draws the death marker and every dropped item through walls.
 *
 * <p>Uses the vanilla gizmo API added in 26.x: {@code Gizmos} plus
 * {@code GizmoProperties#setAlwaysOnTop()}, the same channel vanilla uses for always-visible
 * debug text. Submitted from {@code LevelRenderEvents.BEFORE_GIZMOS}, which fires while the
 * gizmo collector is open.</p>
 *
 * <p>Everything is wrapped defensively: if the collector is ever unavailable the highlight
 * simply does not draw instead of taking the game down.</p>
 */
public final class LegacyGizmoRenderer {

    /** Purple, matching the mod icon's accent. */
    private static final int ICON_COLOR = 0xFFB469FF;
    private static final int ITEM_STROKE = 0xFFB469FF;
    private static final int TEXT_COLOR = 0xFFE3C8FF;
    /**
     * Icon half-size per block of distance. Because the world size grows with the distance, the
     * marker keeps the same apparent size on screen -- tuned to roughly a quarter of a block.
     */
    private static final double ICON_SCREEN_FACTOR = 0.035;

    private static boolean warned;

    private LegacyGizmoRenderer() {
    }

    public static void register() {
        TrackerClientEvents.onGizmos(() -> {
            if (!TrackerModules.isEnabled(ModuleIds.LEGACY_TRACKING)) {
                return;
            }
            DtpNetworking.DeathSnap snap = LegacyHighlightState.active();
            if (snap == null || !LegacyHighlightState.isCurrentDimension(snap)) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) {
                return;
            }
            try {
                draw(level, snap);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] legacy highlight could not draw; disabling it for this session", t);
                }
            }
        });
    }

    private static void draw(ClientLevel level, DtpNetworking.DeathSnap snap) {
        Vec3 centre = new Vec3(snap.x() + 0.5, snap.y() + 0.5, snap.z() + 0.5);

        // Screen-constant marker: the gizmo API is world-space only, so the icon's world size is
        // scaled with the viewing distance. Apparent size then stays the same near and far --
        // a small reticle instead of a wall of box. The shape itself lives in the shared
        // TargetIcon, so 遗产追踪 and 玩家追踪 draw the same mark.
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 eye = player == null ? centre.add(0.0, 0.0, 8.0) : player.getEyePosition();
        double radius = TargetIcon.screenConstantRadius(centre, eye, ICON_SCREEN_FACTOR);
        TargetIcon.draw(centre, radius, eye, ICON_COLOR);

        // The countdown only while there is one: after the window closes the marker is purely
        // "the spot is here", which is still useful for walking back.
        String caption = snap.remainingTicks() > 0
                ? snap.id() + "  " + formatTime(snap.remainingTicks()) : snap.id();
        Gizmos.billboardText(caption, centre.add(0.0, radius * 1.9, 0.0),
                        TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.9f))
                .setAlwaysOnTop();

        // every drop that is still there, found by the uuid the server sent
        for (UUID uuid : snap.itemIds()) {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof ItemEntity item)) {
                continue;
            }
            Gizmos.cuboid(item.getBoundingBox().inflate(0.12), GizmoStyle.stroke(ITEM_STROKE, 2.0f))
                    .setAlwaysOnTop();

            String label = item.getItem().getHoverName().getString()
                    + " x" + item.getItem().getCount()
                    + "  " + formatTime(snap.remainingTicks());
            Gizmos.billboardText(label, item.position().add(0.0, 0.9, 0.0),
                            TextGizmo.Style.forColorAndCentered(TEXT_COLOR).withScale(0.85f))
                    .setAlwaysOnTop();
        }
    }

    /** 6000 ticks -> "5:00". */
    public static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
