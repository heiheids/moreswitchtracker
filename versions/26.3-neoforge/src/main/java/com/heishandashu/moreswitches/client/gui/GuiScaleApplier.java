package com.heishandashu.moreswitches.client.gui;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import net.minecraft.client.Minecraft;

/**
 * Applies a GUI-scale change on the next client tick.
 *
 * <p>Resizing the interface rebuilds the screen that holds the control the player just clicked, so
 * doing it from inside that control's callback means removing the widget while it is still running.
 * Vanilla's own scale option survives that because the screen rebuild is its only side effect; here
 * the change also has to update {@link FreeGuiScale} and the saved settings, so the two are split:
 * the page records the choice, this class performs the resize once, from the tick loop.</p>
 *
 * <p>Deferring also collapses a burst of clicks into a single resize, which matters because each
 * resize reallocates the main render target.</p>
 */
public final class GuiScaleApplier {

    private static boolean pending;

    private GuiScaleApplier() {
    }

    /** Records that the window should pick up a new scale on the next tick. */
    public static void requestApply() {
        pending = true;
    }

    /** Called once per client tick; does nothing unless a change was requested. */
    public static void applyIfPending() {
        if (!pending) {
            return;
        }
        pending = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            // Too early or already shutting down; the next real resize will pick the value up.
            return;
        }
        minecraft.resizeGui();
        MoreSwitchesMod.LOGGER.info("[{}] GUI scale applied: {}x (scaled {}x{})",
                MoreSwitchesMod.MOD_ID, FreeGuiScale.active() ? FreeGuiScale.scale() : 0.0F,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }
}
