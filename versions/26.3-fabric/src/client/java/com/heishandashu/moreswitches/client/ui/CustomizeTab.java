package com.heishandashu.moreswitches.client.ui;

import com.heishandashu.moreswitches.client.outline.HighContrastOutline;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.OutlineColorStyle;
import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The 更多自定义 tab: 原版高对比度方块轮廓的开关与描边颜色。
 *
 * <p>布局是「一行一个设置」：左边标题，右边控件，整体装在一个带上下分隔线的方框里。
 * 开关直接读写原版的 {@code highContrastBlockOutline} 选项，所以和「辅助功能」页面里的
 * 同名选项是同一个开关，两边随时同步；颜色只替换那条彩色细线，黑色衬边保留。</p>
 *
 * <p>以后加设置项只需要在 {@link #ROW_LABEL_KEYS} 里加一行、在 {@code init} 里给它摆控件，
 * 行高和方框会自己算，不用动外壳。</p>
 */
final class CustomizeTab implements SettingsTab {

    private static final int PADDING_X = 24;
    private static final int ROW_HEIGHT = 26;
    private static final int BOX_PADDING_Y = 5;
    private static final int BOX_PADDING_X = 8;
    private static final int LABEL_GAP = 12;

    private static final int TOGGLE_WIDTH = 44;
    private static final int TOGGLE_HEIGHT = 18;
    private static final int SWATCH_WIDTH = 26;
    private static final int SWATCH_HEIGHT = 18;
    private static final int SWATCH_GAP = 4;

    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_DIVIDER = 0x30FFFFFF;
    private static final int BOX_BACKGROUND = 0x12FFFFFF;

    /** 每行左侧的标题键，顺序即显示顺序。加设置项时在这里加一行即可。 */
    private static final String[] ROW_LABEL_KEYS = {
            "moreswitchtracker.customize.high_contrast",
            "moreswitchtracker.customize.color",
            "moreswitchtracker.customize.attack_range"
    };

    private static final int ROW_HIGHLIGHT = 0;
    private static final int ROW_COLOR = 1;
    private static final int ROW_ATTACK_RANGE = 2;

    private static final OutlineColorStyle[] COLORS = {
            OutlineColorStyle.VANILLA,
            OutlineColorStyle.LIME,
            OutlineColorStyle.ORANGE,
            OutlineColorStyle.PURPLE
    };

    private SettingsScreen host;
    private final List<ColorOptionButton> colorButtons = new ArrayList<>();

    private int boxTop;
    private int boxBottom;
    private int boxLeft;
    private int boxRight;

    @Override
    public String titleKey() {
        return "moreswitchtracker.tab.customize";
    }

    @Override
    public String hintKey() {
        return "moreswitchtracker.hint.customize";
    }

    @Override
    public void init(SettingsScreen host) {
        this.host = host;
        this.boxLeft = PADDING_X;
        this.boxRight = host.guiWidth() - PADDING_X;
        this.boxTop = host.contentTop() + 12;
        int rows = ROW_LABEL_KEYS.length;
        this.boxBottom = this.boxTop + BOX_PADDING_Y * 2 + rows * ROW_HEIGHT;

        ToggleSwitchWidget toggle = new ToggleSwitchWidget(
                this.boxRight - BOX_PADDING_X - TOGGLE_WIDTH,
                rowCenterY(ROW_HIGHLIGHT) - TOGGLE_HEIGHT / 2,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                Component.literal(ModLang.text(ROW_LABEL_KEYS[ROW_HIGHLIGHT])),
                HighContrastOutline::enabled,
                HighContrastOutline::setEnabled);
        toggle.setTooltip(Tooltip.create(Component.literal(
                ModLang.text("moreswitchtracker.customize.high_contrast.desc"))));
        host.addTabWidget(toggle);

        ToggleSwitchWidget attackRange = new ToggleSwitchWidget(
                this.boxRight - BOX_PADDING_X - TOGGLE_WIDTH,
                rowCenterY(ROW_ATTACK_RANGE) - TOGGLE_HEIGHT / 2,
                TOGGLE_WIDTH,
                TOGGLE_HEIGHT,
                Component.literal(ModLang.text(ROW_LABEL_KEYS[ROW_ATTACK_RANGE])),
                () -> ModSettings.get().attackRangeHighlight(),
                value -> ModSettings.get().setAttackRangeHighlight(value));
        attackRange.setTooltip(Tooltip.create(Component.literal(
                ModLang.text("moreswitchtracker.customize.attack_range.desc"))));
        host.addTabWidget(attackRange);

        int groupWidth = COLORS.length * SWATCH_WIDTH + (COLORS.length - 1) * SWATCH_GAP;
        int groupX = this.boxRight - BOX_PADDING_X - groupWidth;
        int swatchY = rowCenterY(ROW_COLOR) - SWATCH_HEIGHT / 2;

        this.colorButtons.clear();
        for (int index = 0; index < COLORS.length; index++) {
            OutlineColorStyle style = COLORS[index];
            int x = groupX + index * (SWATCH_WIDTH + SWATCH_GAP);
            ColorOptionButton button = new ColorOptionButton(
                    x, swatchY, SWATCH_WIDTH, SWATCH_HEIGHT,
                    style.argb(),
                    () -> ModSettings.get().outlineColor() == style,
                    () -> ModSettings.get().setOutlineColor(style),
                    Component.literal(colorName(style)));
            button.setTooltip(Tooltip.create(Component.literal(colorTooltip(style))));
            this.colorButtons.add(button);
            host.addTabWidget(button);
        }
    }

    /** 第 {@code row} 行控件的垂直中线。 */
    private int rowCenterY(int row) {
        return this.boxTop + BOX_PADDING_Y + row * ROW_HEIGHT + ROW_HEIGHT / 2;
    }

    private static String colorName(OutlineColorStyle style) {
        return ModLang.text("moreswitchtracker.customize.color." + style.id());
    }

    /** 色块没有文字，名称和「先开开关」的提示都放在悬停提示里，省一行高度。 */
    private static String colorTooltip(OutlineColorStyle style) {
        String name = colorName(style);
        if (HighContrastOutline.enabled()) {
            return name;
        }
        return name + "\n" + ModLang.text("moreswitchtracker.customize.color.off_hint");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float partialTick) {
        graphics.fill(this.boxLeft, this.boxTop, this.boxRight, this.boxBottom, BOX_BACKGROUND);
        graphics.fill(this.boxLeft, this.boxTop, this.boxRight, this.boxTop + 1, TEXT_DIVIDER);
        graphics.fill(this.boxLeft, this.boxBottom - 1, this.boxRight, this.boxBottom,
                TEXT_DIVIDER);

        int labelX = this.boxLeft + BOX_PADDING_X;
        int labelMaxWidth = Math.max(1, this.boxRight - this.boxLeft - BOX_PADDING_X * 2
                - TOGGLE_WIDTH - LABEL_GAP);
        for (int row = 0; row < ROW_LABEL_KEYS.length; row++) {
            int textY = rowCenterY(row) - 4;
            graphics.text(this.host.guiFont(),
                    fit(ModLang.text(ROW_LABEL_KEYS[row]), labelMaxWidth),
                    labelX, textY, TEXT_PRIMARY);
            if (row > 0) {
                int dividerY = this.boxTop + BOX_PADDING_Y + row * ROW_HEIGHT;
                graphics.fill(this.boxLeft + BOX_PADDING_X, dividerY,
                        this.boxRight - BOX_PADDING_X, dividerY + 1, TEXT_DIVIDER);
            }
        }
    }

    private String fit(String text, int maxWidth) {
        if (this.host.guiFont().width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - this.host.guiFont().width(ellipsis);
        if (budget <= 0) {
            return "";
        }
        return this.host.guiFont().plainSubstrByWidth(text, budget) + ellipsis;
    }
}
