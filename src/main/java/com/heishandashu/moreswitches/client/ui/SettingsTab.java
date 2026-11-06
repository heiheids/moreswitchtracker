package com.heishandashu.moreswitches.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One page inside the settings screen.
 *
 * <p>The shell owns the window frame, the tab bar, the language button and the Done button. A tab
 * owns only its own list and controls, which is what keeps the two former mods from turning into
 * one giant screen class.</p>
 *
 * <p>A tab is re-initialised on every {@link SettingsScreen#rebuildWidgets()}. It must add its
 * widgets through the host, because the host clears the widget list between tabs.</p>
 */
public interface SettingsTab {

    /** Translation key of the tab label shown in the top bar. */
    String titleKey();

    /** Translation key of the footer hint; {@code %s} is replaced with the bound key. */
    String hintKey();

    /** Creates this tab's widgets for the current window size. */
    void init(SettingsScreen host);

    /** Draws text and decoration. Widgets themselves are drawn by the host. */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** Returns true when the tab handled the wheel event. */
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    /** Returns true when the tab handled the click (e.g. on its scrollbar). */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Called after the screen released the mouse, so a finished drag can be committed once. */
    default void mouseReleased() {
    }

    /** Offered the key before the focused widget sees it; true means the tab consumed it. */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }
}
