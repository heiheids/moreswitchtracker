package com.heishandashu.moreswitches.config;

import java.util.List;

/**
 * Canonical list of switches, in the order they appear in the settings screen.
 *
 * <p>Row order in the UI is exactly this order, so "add a feature" means: append here, add its
 * module class, add its language keys. Nothing else in the UI changes.</p>
 */
public final class SwitchIds {

    /** Freeze the game window's size. */
    public static final String WINDOW_LOCK = "window_lock";

    /** Force a three-second cooldown between firework rocket uses. */
    public static final String FIREWORK_COOLDOWN = "firework_cooldown";

    /** Allow sleeping during the day while it rains. */
    public static final String RAIN_SLEEP = "rain_sleep";

    /** Ignore the drop key, so a mis-press cannot throw an item away. */
    public static final String DROP_KEY_LOCK = "drop_key_lock";

    /** Ignore the full-screen key, so an accidental press cannot change the mode. */
    public static final String FULLSCREEN_KEY_LOCK = "fullscreen_key_lock";

    /** Hide status-effect particles without touching the effects themselves. */
    public static final String EFFECT_PARTICLES_OFF = "effect_particles_off";

    /** Draw light-level numbers for the block a held light source would light. */
    public static final String LIGHT_PREVIEW = "light_preview";

    /** Brighten the surroundings of a held light source, visually only. */
    public static final String MOVING_LIGHT = "moving_light";

    /** Skip the version-mismatch warning when an incompatible resource pack is enabled. */
    public static final String RESOURCE_PACK_WARNING_OFF = "resource_pack_warning_off";

    /** Stop campfires from producing any smoke. */
    public static final String CAMPFIRE_SMOKE_OFF = "campfire_smoke_off";

    /** Never start the tutorial, so its toasts never appear. */
    public static final String TUTORIAL_OFF = "tutorial_off";

    /** Set Carpet's {@code commandPlayer} rule so fake players can be used in survival. */
    public static final String CARPET_FAKE_PLAYER = "carpet_fake_player";

    /** Keep a totem of undying in the offhand whenever one is available. */
    public static final String TOTEM_REFILL = "totem_refill";

    /** Drop the explosion boom. */
    public static final String TNT_SOUND_OFF = "tnt_sound_off";

    /** Drop minecart rolling and riding sounds. */
    public static final String MINECART_SOUND_OFF = "minecart_sound_off";

    /** Drop piston extend and contract sounds. */
    public static final String PISTON_SOUND_OFF = "piston_sound_off";

    private static final List<SwitchDefinition> ALL = List.of(
            new SwitchDefinition(WINDOW_LOCK, false),
            new SwitchDefinition(FIREWORK_COOLDOWN, false),
            new SwitchDefinition(RAIN_SLEEP, false),
            new SwitchDefinition(DROP_KEY_LOCK, false),
            new SwitchDefinition(FULLSCREEN_KEY_LOCK, false),
            new SwitchDefinition(EFFECT_PARTICLES_OFF, false),
            new SwitchDefinition(LIGHT_PREVIEW, false),
            new SwitchDefinition(MOVING_LIGHT, false),
            new SwitchDefinition(RESOURCE_PACK_WARNING_OFF, false),
            new SwitchDefinition(CAMPFIRE_SMOKE_OFF, false),
            new SwitchDefinition(TUTORIAL_OFF, false),
            new SwitchDefinition(CARPET_FAKE_PLAYER, false),
            new SwitchDefinition(TOTEM_REFILL, false),
            new SwitchDefinition(TNT_SOUND_OFF, false),
            new SwitchDefinition(MINECART_SOUND_OFF, false),
            new SwitchDefinition(PISTON_SOUND_OFF, false));

    private SwitchIds() {
    }

    public static List<SwitchDefinition> all() {
        return ALL;
    }
}
