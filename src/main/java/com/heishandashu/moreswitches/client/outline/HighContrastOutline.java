package com.heishandashu.moreswitches.client.outline;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.OutlineColorStyle;

/**
 * 1.21.1 的高对比度方块轮廓：本模组自己实现，因为这一代原版没有该功能。
 *
 * <p>开关与颜色都存在 {@code config/moreswitches.json}，由 {@code MixinLevelRendererOutline}
 * 在渲染描边时读取。绘制方式与后来的原版一致：一条加粗的黑线衬底 + 一条彩色细线。</p>
 */
public final class HighContrastOutline {

    /** 黑色衬底向外的扩张量（单位：格）。比线条本身粗，视觉上就是「加粗黑边」。 */
    public static final double BACKING_INFLATE = 0.008D;

    /** 黑色衬底的不透明度。 */
    public static final float BACKING_ALPHA = 0.9F;

    /** 彩色描边的不透明度。 */
    public static final float LINE_ALPHA = 0.95F;

    private HighContrastOutline() {
    }

    public static boolean enabled() {
        return ModSettings.get().highContrastOutline();
    }

    public static void setEnabled(boolean value) {
        ModSettings.get().setHighContrastOutline(value);
    }

    /** 当前选择的描边颜色（ARGB）。原版色在 1.21.1 上同样使用那一代后来引入的青绿色。 */
    public static int color() {
        return ModSettings.get().outlineColor().argb();
    }

    public static float red() {
        return ((color() >> 16) & 0xFF) / 255.0F;
    }

    public static float green() {
        return ((color() >> 8) & 0xFF) / 255.0F;
    }

    public static float blue() {
        return (color() & 0xFF) / 255.0F;
    }

    /** 是否选择了自定义颜色；{@link OutlineColorStyle#VANILLA} 表示用默认青绿色。 */
    public static boolean customColorSelected() {
        return ModSettings.get().outlineColor().overridesVanilla();
    }
}
