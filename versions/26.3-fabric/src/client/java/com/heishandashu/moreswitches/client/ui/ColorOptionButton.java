package com.heishandashu.moreswitches.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * 一个带色块的描边颜色选项。
 *
 * <p>色块本身就是预览；能放下文字时显示名称，放不下时只显示色块、名称交给悬停提示。
 * 选中时整圈加白边。控件不保存状态：是否选中由外部通过 {@code selected} 实时查询。</p>
 */
final class ColorOptionButton extends AbstractWidget {

    private static final int BACKGROUND = 0xFF1E1E1E;
    private static final int BACKGROUND_HOVER = 0xFF3A3A3A;
    private static final int BORDER = 0xFF000000;
    private static final int BORDER_SELECTED = 0xFFFFFFFF;
    private static final int SWATCH_BORDER = 0xC0000000;
    private static final int TEXT = 0xFFD0D0D0;
    private static final int TEXT_SELECTED = 0xFFFFFFFF;

    private static final int SWATCH_SIZE = 12;
    private static final int SWATCH_INSET = 4;
    private static final int LABEL_INSET = 6;

    private final int swatchArgb;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    ColorOptionButton(int x, int y, int width, int height, int swatchArgb,
                      BooleanSupplier selected, Runnable onPress, Component narration) {
        super(x, y, width, height, narration);
        this.swatchArgb = swatchArgb;
        this.selected = selected;
        this.onPress = onPress;
    }

    /** 只有宽度足够放下文字时才画名称，紧凑模式下只留色块。 */
    private boolean drawsLabel() {
        return getWidth() >= SWATCH_SIZE + SWATCH_INSET * 2 + LABEL_INSET
                + Minecraft.getInstance().font.width(getMessage()) + 4;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onPress.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        boolean isSelected = this.selected.getAsBoolean();
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1,
                isHovered() ? BACKGROUND_HOVER : BACKGROUND);

        int swatchX = x + SWATCH_INSET;
        int swatchY = y + (height - SWATCH_SIZE) / 2;
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE,
                SWATCH_BORDER);
        graphics.fill(swatchX + 1, swatchY + 1, swatchX + SWATCH_SIZE - 1,
                swatchY + SWATCH_SIZE - 1, this.swatchArgb);

        if (drawsLabel()) {
            int textX = swatchX + SWATCH_SIZE + LABEL_INSET;
            int textY = y + (height - 8) / 2;
            graphics.text(Minecraft.getInstance().font, getMessage().getString(), textX, textY,
                    isSelected ? TEXT_SELECTED : TEXT);
        }

        if (isSelected) {
            graphics.fill(x, y, x + width, y + 1, BORDER_SELECTED);
            graphics.fill(x, y + height - 1, x + width, y + height, BORDER_SELECTED);
            graphics.fill(x, y, x + 1, y + height, BORDER_SELECTED);
            graphics.fill(x + width - 1, y, x + width, y + height, BORDER_SELECTED);
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
