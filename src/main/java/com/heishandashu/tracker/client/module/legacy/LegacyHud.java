package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.client.render.EdgeArrow;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.DtpNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The countdown and the edge arrow for 遗产追踪.
 *
 * <p>26.3 drew the countdown as world-space gizmo text above the marker. 1.21.1 has no such API,
 * so the same text is drawn as a HUD label at the marker's projected position, and becomes an edge
 * arrow when the marker is off screen -- which is arguably more useful, since the death point is
 * usually what you are walking towards rather than something already in view.</p>
 */
public final class LegacyHud {

    private static final int COUNTDOWN_COLOR = 0xFFE3C8FF;
    private static final int ARROW_COLOR = 0xFFB469FF;
    private static final double ARROW_MARGIN = 18.0;

    private LegacyHud() {
    }

    public static void register() {
        ClientEvents.addHudPass((graphics, partialTick) -> render(graphics));
    }

    private static void render(net.minecraft.client.gui.GuiGraphics graphics) {
        if (!TrackerModules.isEnabled(ModuleIds.LEGACY_TRACKING)) {
            return;
        }
        DtpNetworking.DeathSnap snap = LegacyHighlightState.active();
        if (snap == null || !LegacyHighlightState.isCurrentDimension(snap)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        ClientLevel level = minecraft.level;
        if (self == null || level == null) {
            return;
        }

        Vec3 centre = new Vec3(snap.x() + 0.5, snap.y() + 0.5, snap.z() + 0.5);
        String caption = snap.remainingTicks() > 0
                ? snap.id() + "  " + LegacyGizmoRenderer.formatTime(snap.remainingTicks())
                : snap.id();

        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        ScreenProjection.Projected projected =
                ScreenProjection.project(eye, forward, fov, width, height, centre, ARROW_MARGIN);
        if (projected.onScreen()) {
            com.heishandashu.tracker.client.render.WorldLabel.draw(
                    graphics, eye, forward, fov, centre.add(0.0, 0.9, 0.0), caption, COUNTDOWN_COLOR);
        } else {
            EdgeArrow.draw(graphics, minecraft.font, width, height,
                    projected.screenX(), projected.screenY(),
                    projected.dirX(), projected.dirY(), caption, ARROW_COLOR);
        }
    }
}
