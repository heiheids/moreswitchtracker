package com.heishandashu.tracker.client.module.legacy;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.DtpNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Small on-screen countdown for the highlighted death, so the timer is readable without
 * walking back to the drop site.
 */
public final class LegacyHud {

    private static final int X = 6;
    private static final int TOP = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int LABEL_COLOR = 0xFF9BFF9B;

    private LegacyHud() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!TrackerModules.isEnabled(ModuleIds.LEGACY_TRACKING) || !LegacyHighlightState.hasAnything()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int y = TOP;

        DtpNetworking.DeathSnap active = LegacyHighlightState.active();
        for (DtpNetworking.DeathSnap snap : LegacyHighlightState.all()) {
            // Only what is in front of the player: a death in another dimension cannot be walked to.
            if (!LegacyHighlightState.isCurrentDimension(snap)) {
                continue;
            }
            boolean isActive = active != null && active.id().equals(snap.id());
            boolean hasItems = !snap.itemIds().isEmpty();
            // Once the window is over the countdown is noise: the death point is still worth
            // walking back to, but there is no time left to show.
            if (!isActive && !hasItems && snap.remainingTicks() <= 0) {
                continue;
            }

            String label = (isActive ? "▶ " : "  ") + snap.id();
            if (snap.remainingTicks() > 0) {
                label += "  " + LegacyGizmoRenderer.formatTime(snap.remainingTicks());
            }
            if (hasItems) {
                label += "  (" + snap.itemIds().size() + ")";
            }
            Component line = Component.literal(label);
            graphics.text(font, line, X, y, isActive ? 0xFFFFFFFF : LABEL_COLOR);
            y += LINE_HEIGHT;
        }
    }
}
