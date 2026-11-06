package com.heishandashu.tracker.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Entity lookup by UUID, which 1.21.1 cannot do directly.
 *
 * <p>26.3 added {@code Level#getEntity(UUID)}. On 1.21.1 the only public lookup is
 * {@code getEntity(int)} (the network id), so the client has to walk the entity list instead.
 * Several features need "the live entity behind this UUID, if the client has it"; centralising
 * the walk keeps that one idiom in one place.</p>
 *
 * <p>Returns null when the entity is not loaded here, which is the normal case for anything
 * outside the client's entity-sync range -- callers fall back to the server-sent coordinates.</p>
 */
public final class EntityLookup {

    private EntityLookup() {
    }

    /** The loaded entity with this UUID, or null. */
    public static Entity byUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (uuid.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }
}
