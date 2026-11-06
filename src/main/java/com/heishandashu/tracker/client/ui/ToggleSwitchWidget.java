package com.heishandashu.tracker.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A left/right sliding switch, drawn in the vanilla widget palette.
 *
 * <p>Minecraft has no switch widget of its own, so this draws one with plain {@code fill}
 * rectangles: a beveled track plus a beveled knob. The widget owns no state -- it reads the
 * current value through {@code current} and reports flips through {@code onToggle}.</p>
 *
 * <p>1.21.1 uses the immediate {@code render} model; 26.x replaced it with
 * {@code extractWidgetRenderState}. Same drawing code, different override.</p>
 */
public class ToggleSwitchWidget extends AbstractWidget {

    private static final int TRACK_ON = 0xFF3C8527;         // vanilla-ish green
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

    private static final int KNOB_WIDTH = 16;

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
    public void onClick(double mouseX, double mouseY) {
        this.onToggle.accept(!value());
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean on = value();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        int track = on ? TRACK_ON : TRACK_OFF;
        int trackLight = on ? TRACK_ON_LIGHT : TRACK_OFF_LIGHT;
        int trackDark = on ? TRACK_ON_DARK : TRACK_OFF_DARK;

        // drop shadow, then the beveled track
        g.fill(x, y + 1, x + w, y + h + 1, SHADOW);
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, track);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, trackLight);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, trackLight);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, trackDark);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, trackDark);

        // knob sits left when off, right when on
        int knobX = on ? x + w - KNOB_WIDTH - 2 : x + 2;
        int knobY = y + 2;
        int knobH = h - 4;
        g.fill(knobX, knobY, knobX + KNOB_WIDTH, knobY + knobH, OUTLINE);
        g.fill(knobX + 1, knobY + 1, knobX + KNOB_WIDTH - 1, knobY + knobH - 1, KNOB);
        g.fill(knobX + 1, knobY + 1, knobX + KNOB_WIDTH - 1, knobY + 2, KNOB_LIGHT);
        g.fill(knobX + 1, knobY + 1, knobX + 2, knobY + knobH - 1, KNOB_LIGHT);
        g.fill(knobX + 1, knobY + knobH - 2, knobX + KNOB_WIDTH - 1, knobY + knobH - 1, KNOB_DARK);
        g.fill(knobX + KNOB_WIDTH - 2, knobY + 1, knobX + KNOB_WIDTH - 1, knobY + knobH - 1, KNOB_DARK);

        // vanilla widgets light up under the cursor
        if (isHovered()) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, HOVER_OVERLAY);
        }
        if (!this.active) {
            g.fill(x, y, x + w, y + h, 0x80000000);
        }
    }

    /** Screen readers: reuse the vanilla widget narration so the switch reads its label. */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
