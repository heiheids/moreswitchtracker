package com.heishandashu.moreswitches.client.outline;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.OutlineColorStyle;
import net.minecraft.client.Minecraft;

/**
 * 决定高对比度方块轮廓使用哪一种颜色。
 *
 * <p>原版在 {@code LevelRenderer.submitHitOutline} 里画两条线：一条加粗的黑线衬底，一条彩色细线。
 * 这里只替换那条彩色细线，衬底仍然是黑色，所以换成黄绿、橘色、紫色后依然醒目。</p>
 */
public final class HighContrastOutline {

    /** 原版彩色细线的颜色，取自 26.3 的字节码常量 -11010079。 */
    public static final int VANILLA_COLOR = 0xFF57FFE1;

    private HighContrastOutline() {
    }

    /** 当前是否启用了自定义颜色（选择了原版颜色时为 false）。 */
    public static boolean customColorSelected() {
        return ModSettings.get().outlineColor().overridesVanilla();
    }

    /**
     * 把原版要用的描边颜色换成设置里的颜色。
     *
     * <p>只有颜色恰好等于原版高对比度色时才替换，因此调试形状用的白色描边、非高对比度模式下的
     * 半透明黑边都不会被误改。</p>
     */
    public static int colorFor(int vanillaColor) {
        if (vanillaColor != VANILLA_COLOR) {
            return vanillaColor;
        }
        OutlineColorStyle style = ModSettings.get().outlineColor();
        return style.overridesVanilla() ? style.argb() : vanillaColor;
    }

    /** 原版开关的当前状态。每次实时读取，所以原版辅助功能页面改动后会立刻同步。 */
    public static boolean enabled() {
        return Boolean.TRUE.equals(
                Minecraft.getInstance().options.highContrastBlockOutline().get());
    }

    /** 打开或关闭原版的高对比度方块轮廓，并立刻写入 options.txt。 */
    public static void setEnabled(boolean value) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.highContrastBlockOutline().set(value);
        minecraft.options.save();
    }
}
