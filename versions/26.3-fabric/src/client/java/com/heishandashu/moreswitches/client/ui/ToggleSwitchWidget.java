package com.heishandashu.moreswitches.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A sliding on/off switch, ported to 26.3's extract-render-state widget contract.
 *
 * <p>The widget owns no state: it reads through {@code current} and reports flips through
 * {@code onToggle}, so the same class drives both halves' settings pages.</p>
 */
public class ToggleSwitchWidget extends AbstractWidget {

    private static final int TRACK_ON = 0xFF3C8527;
    private static final int TRACK_ON_LIGHT = 0xFF6FC95A;
    private static final int TRACK_ON_DARK = 0xFF1E4D14;
    private static final int TRACK_OFF = 0xFF565656;
    private static final int TRACK_OFF_LIGHT = 0xFF8A8A8A;
    private static final int TRACK_OFF_DARK = 0xFF2C2C2C;
    private static final int KNOB = 0xFFC6C6C6;
    private static final int KNOB_LIGHT = 0xFFFFFFFF;
    private static final int KNOB_DARK = 0xFF6C6C6C;
    private static final int OUTLINE = 0xFF000000;
    private static final int HOVER_OVERLAY = 0x28FFFFFF;
    private static final int SHADOW = 0x40000000;

    public static final int PREFERRED_HEIGHT = 18;
    public static final int PREFERRED_WIDTH = 44;

    private static final int KNOB_WIDTH = 18;

    private final BooleanSupplier current;
    private final Consumer<Boolean> onToggle;

    public ToggleSwitchWidget(int x, int y, int width, int height, Component narration,
                              BooleanSupplier current, Consumer<Boolean> onToggle) {
        super(x, y, width, height, narration);
        this.current = current;
        this.onToggle = onToggle;
    }

    public boolean value() {
        return this.current.getAsBoolean();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onToggle.accept(!value());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        boolean on = value();
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        int track = on ? TRACK_ON : TRACK_OFF;
        int trackLight = on ? TRACK_ON_LIGHT : TRACK_OFF_LIGHT;
        int trackDark = on ? TRACK_ON_DARK : TRACK_OFF_DARK;

        graphics.fill(x, y + 1, x + width, y + height + 1, SHADOW);
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, track);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, trackLight);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, trackLight);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, trackDark);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, trackDark);

        int knobWidth = Math.min(KNOB_WIDTH, width - 4);
        int knobX = on ? x + width - knobWidth - 2 : x + 2;
        int knobY = y + 2;
        int knobHeight = height - 4;
        graphics.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, OUTLINE);
        graphics.fill(knobX + 1, knobY + 1, knobX + knobWidth - 1, knobY + knobHeight - 1, KNOB);
        graphics.fill(knobX + 1, knobY + 1, knobX + knobWidth - 1, knobY + 2, KNOB_LIGHT);
        graphics.fill(knobX + 1, knobY + 1, knobX + 2, knobY + knobHeight - 1, KNOB_LIGHT);
        graphics.fill(knobX + 1, knobY + knobHeight - 2, knobX + knobWidth - 1,
                knobY + knobHeight - 1, KNOB_DARK);
        graphics.fill(knobX + knobWidth - 2, knobY + 1, knobX + knobWidth - 1,
                knobY + knobHeight - 1, KNOB_DARK);

        if (isHovered()) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, HOVER_OVERLAY);
        }
        if (!this.active) {
            graphics.fill(x, y, x + width, y + height, 0x80000000);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
