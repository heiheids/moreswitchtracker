package com.heishandashu.tracker.client.module.players;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Where a highlighted player actually is, from the client's point of view.
 *
 * <p>Shared by the world marker and the HUD arrow so both agree: the live entity while it is in
 * sync range (smooth, exact), otherwise the position the server pushed, and nothing at all if the
 * player is in another dimension.</p>
 */
public final class PlayerPositions {

    private static final double EYE_HEIGHT = 1.6;

    private PlayerPositions() {
    }

    /** @return the eye-level position to aim at, or null when there is nothing to show */
    static Vec3 resolve(ClientLevel level, String dimension, UUID uuid) {
        AbstractClientPlayer entity = find(level, uuid);
        if (entity != null) {
            return entity.getEyePosition();
        }
        PlayerHighlightState.SyncedPosition synced = PlayerHighlightState.position(uuid);
        if (PlayerHighlightState.sameDimension(synced, dimension)) {
            return synced.pos().add(0.0, EYE_HEIGHT, 0.0);
        }
        return null;
    }

    private static AbstractClientPlayer find(ClientLevel level, UUID uuid) {
        for (AbstractClientPlayer player : level.players()) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }
}
