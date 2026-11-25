package com.heishandashu.moreswitches.config;

import java.util.Locale;

/**
 * 高对比度方块描边可用的颜色。
 *
 * <p>{@link #VANILLA} 表示不覆盖原版颜色：打开开关后仍然使用原版的青绿色描边。
 * 其余几种会替换原版那条彩色描边，黑色衬边保留，所以对比度不会下降。</p>
 */
public enum OutlineColorStyle {

    /** 原版的青绿色（0xFF57FFE1），即不覆盖。 */
    VANILLA("vanilla", 0xFF57FFE1),

    /** 黄绿色。 */
    LIME("lime", 0xFF9BE22A),

    /** 橘色。 */
    ORANGE("orange", 0xFFFF8A1F),

    /** 紫色。 */
    PURPLE("purple", 0xFFB65CFF);

    private final String id;
    private final int argb;

    OutlineColorStyle(String id, int argb) {
        this.id = id;
        this.argb = argb;
    }

    public String id() {
        return this.id;
    }

    /** 实际使用的 ARGB；{@link #VANILLA} 返回的正是原版颜色。 */
    public int argb() {
        return this.argb;
    }

    /** 是否需要用本模组的颜色替换原版颜色。 */
    public boolean overridesVanilla() {
        return this != VANILLA;
    }

    public OutlineColorStyle next() {
        OutlineColorStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** 读取文件里的值；无法识别时回到原版颜色，而不是让配置失败。 */
    public static OutlineColorStyle byId(String raw) {
        if (raw == null) {
            return VANILLA;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (OutlineColorStyle style : values()) {
            if (style.id.equals(value)) {
                return style;
            }
        }
        return VANILLA;
    }
}
