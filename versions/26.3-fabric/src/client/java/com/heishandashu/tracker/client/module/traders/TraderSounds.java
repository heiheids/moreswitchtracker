package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The alert that plays when a wandering trader turns up.
 *
 * <p>The sound lives in the mod's own resources ({@code assets/tracker/sounds/trader_spawn.ogg},
 * Ogg Vorbis -- Minecraft's sound engine cannot read mp3) and is declared in
 * {@code assets/tracker/sounds.json}.</p>
 */
public final class TraderSounds {

    private static final Identifier SOUND_ID =
            Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "trader_spawn");

    private static final SoundEvent TRADER_SPAWN = SoundEvent.createVariableRangeEvent(SOUND_ID);

    private TraderSounds() {
    }

    /** Plays the alert locally; it is a UI sound, so it does not need a world position. */
    public static void playSpawnAlert() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        // Settles "no sound" reports in one line: either the resource pack never registered the
        // event (the asset side) or the request itself never happened (the trigger side).
        WeighedSoundEvents registered = minecraft.getSoundManager().getSoundEvent(SOUND_ID);
        if (registered == null || registered == SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT) {
            TrackerMod.LOGGER.warn("[tracker] sound {} is NOT registered -- check "
                    + "assets/tracker/sounds.json and trader_spawn.ogg", SOUND_ID);
        } else {
            TrackerMod.LOGGER.info("[tracker] trader alert sound requested ({}) -- registered", SOUND_ID);
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TRADER_SPAWN, 1.0f, 1.0f));
    }
}
