package com.heishandashu.moreswitches.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * One page inside the 26.3 settings screen.
 *
 * <p>26.3's render contract is extraction, not immediate drawing, so a tab receives
 * {@link GuiGraphicsExtractor}. The shell owns the frame; the tab owns its rows and controls.</p>
 */
public interface SettingsTab {

    String titleKey();

    String hintKey();

    void init(SettingsScreen host);

    void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                            float partialTick);

    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    default boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    /** Called after the screen has released the mouse, so a drag can be committed once. */
    default void mouseReleased() {
    }

    /** Offered the key before the focused widget sees it; true means the tab consumed it. */
    default boolean keyPressed(KeyEvent event) {
        return false;
    }
}
