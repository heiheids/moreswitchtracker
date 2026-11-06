package com.heishandashu.moreswitches.sound;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * Decides which sound events the three "silence this" switches drop.
 *
 * <p>Matching is done on the sound identifier rather than on a {@code SoundEvent} object, because
 * the same sound reaches the engine through several paths — a positional packet, an entity-bound
 * packet, and long-lived sound instances such as the minecart rattle. All of them carry the
 * identifier, and only some carry the event object.</p>
 *
 * <p>The identifiers are spelled out here so the mapping can be unit tested without a running
 * game. They are stable resource names, not implementation details.</p>
 */
public final class SoundRules {

    /**
     * The shared explosion boom.
     *
     * <p>Minecraft has one event for explosion sounds, used by TNT as well as by creepers, beds and
     * respawn anchors, so silencing it removes every explosion boom rather than TNT's alone. There
     * is no per-source variant to key on.</p>
     */
    public static final String EXPLODE = "minecraft:entity.generic.explode";

    /** Rolling noise of a moving minecart. */
    public static final String MINECART_RIDING = "minecraft:entity.minecart.riding";

    /** Muffled noise heard while riding inside a minecart. */
    public static final String MINECART_INSIDE = "minecraft:entity.minecart.inside";

    /** The same, muffled further when the ride is underwater. */
    public static final String MINECART_INSIDE_UNDERWATER =
            "minecraft:entity.minecart.inside.underwater";

    /** Piston pushing out. */
    public static final String PISTON_EXTEND = "minecraft:block.piston.extend";

    /** Piston pulling back. */
    public static final String PISTON_CONTRACT = "minecraft:block.piston.contract";

    private SoundRules() {
    }

    /**
     * Whether the given sound identifier should be silenced.
     *
     * <p>An unknown identifier is never silenced, so unrelated sounds are untouched.</p>
     */
    public static boolean blocked(String soundId) {
        if (soundId == null) {
            return false;
        }
        return switch (soundId) {
            case EXPLODE -> ModSettings.get().isEnabled(SwitchIds.TNT_SOUND_OFF);
            case MINECART_RIDING, MINECART_INSIDE, MINECART_INSIDE_UNDERWATER ->
                    ModSettings.get().isEnabled(SwitchIds.MINECART_SOUND_OFF);
            case PISTON_EXTEND, PISTON_CONTRACT ->
                    ModSettings.get().isEnabled(SwitchIds.PISTON_SOUND_OFF);
            default -> false;
        };
    }

    /**
     * Whether this identifier belongs to one of the sounds the switches know about.
     *
     * <p>Separate from {@link #blocked(String)} so the identifier table itself can be verified
     * against the three switch values.</p>
     */
    public static boolean isKnown(String soundId) {
        if (soundId == null) {
            return false;
        }
        return switch (soundId) {
            case EXPLODE, MINECART_RIDING, MINECART_INSIDE, MINECART_INSIDE_UNDERWATER,
                 PISTON_EXTEND, PISTON_CONTRACT -> true;
            default -> false;
        };
    }

    /** Whether any of the three sound switches is on, so the hot path can be skipped entirely. */
    public static boolean anyEnabled() {
        return ModSettings.get().isEnabled(SwitchIds.TNT_SOUND_OFF)
                || ModSettings.get().isEnabled(SwitchIds.MINECART_SOUND_OFF)
                || ModSettings.get().isEnabled(SwitchIds.PISTON_SOUND_OFF);
    }
}
