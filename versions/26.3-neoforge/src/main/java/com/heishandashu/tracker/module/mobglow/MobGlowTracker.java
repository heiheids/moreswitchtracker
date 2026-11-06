package com.heishandashu.tracker.module.mobglow;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.compat.TrackerEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server half of 怪物透视: lights every hostile mob within {@link MobGlowTrackingModule#RADIUS}
 * of any player and keeps it lit for two minutes.
 *
 * <h2>Why the timer is tracked per mob instead of just re-setting the flag</h2>
 *
 * <p>{@code setGlowingTag(true)} has no duration -- it stays until something clears it. If every
 * scan simply re-set it, the flag would stay on forever after the mob left the radius, and the
 * "2 minutes" would be meaningless. If instead the scan cleared everything it did not touch this
 * tick, a mob walking in and out of the radius would flicker.</p>
 *
 * <p>So each lit mob gets an expiry tick. Within the window it stays lit no matter where it goes
 * (including out of the radius -- that is the point of a timed highlight). Once the expiry passes
 * the flag is cleared, unless the mob is still in range, in which case it is re-lit immediately
 * and gets a fresh window. Mob deaths and chunk unloads are not special-cased: the flag dies with
 * the entity, and this tracker only ever looks at entities the server still loads.</p>
 *
 * <h2>Scope</h2>
 *
 * <p>Only {@link Mob}s that implement {@link Enemy} are lit -- that is vanilla's own definition of
 * "hostile", so it covers zombies/skeletons/creepers/blazes/creakings and the like and excludes
 * animals, villagers and the player. A dedicated server with no players does nothing.</p>
 */
public final class MobGlowTracker {

    /** entity uuid -> server tick at which the current glow expires. */
    private static final Map<UUID, Long> LIT = new HashMap<>();
    /** Entities we set the flag on, so we only ever clear a flag we own. */
    private static final Set<UUID> OURS = new HashSet<>();

    private static int scanTimer;
    private static boolean initialized;
    private static boolean warned;
    /**
     * Whether {@code isCurrentlyGlowing()} was observed true at least once. That method is what
     * vanilla consults when deciding to render the outline, so this is the difference between
     * "we set a flag" and "the outline really shows".
     */
    private static boolean glowingVerified;
    /** The server this tracker last ticked on, so a switch-off can clear the flags it set. */
    private static MinecraftServer currentServer;

    private MobGlowTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // Fabric's ServerTickEvents.END_SERVER_TICK; NeoForge spells it ServerTickEvent.Post.
        TrackerEvents.onServerTick(MobGlowTracker::serverTick);
    }

    /**
     * Clears every flag this module set. Called when the switch is turned off, so turning the
     * feature off does not leave the world permanently glowing.
     */
    public static void clearAll(MinecraftServer server) {
        MinecraftServer target = server != null ? server : currentServer;
        if (target != null) {
            for (ServerLevel level : target.getAllLevels()) {
                for (UUID uuid : OURS) {
                    Entity entity = level.getEntity(uuid);
                    if (entity != null) {
                        entity.setGlowingTag(false);
                    }
                }
            }
        }
        LIT.clear();
        OURS.clear();
    }

    private static void serverTick(MinecraftServer server) {
        currentServer = server;
        // Expire timers every tick: this part is cheap and independent of the scan interval,
        // so a glow ends on time instead of on the next scan boundary.
        long now = server.getTickCount();
        expireDue(server, now);

        if (!TrackerModules.isEnabled(ModuleIds.MOB_GLOW_TRACKING)) {
            return;
        }
        if (++scanTimer < MobGlowTrackingModule.SCAN_INTERVAL) {
            return;
        }
        scanTimer = 0;

        try {
            scan(server, now);
        } catch (Throwable t) {
            // A scan bug must never take the server down: report once, then keep ticking.
            if (!warned) {
                warned = true;
                TrackerMod.LOGGER.warn("[tracker] mob glow scan failed; will keep retrying", t);
            }
        }
    }

    /** Clears the flag for every mob whose window has run out. */
    private static void expireDue(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, Long>> it = LIT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() > now) {
                continue;
            }
            UUID uuid = entry.getKey();
            it.remove();
            Entity entity = findEntity(server, uuid);
            if (entity != null && OURS.remove(uuid)) {
                entity.setGlowingTag(false);
            } else {
                OURS.remove(uuid);
            }
        }
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static void scan(MinecraftServer server, long now) {
        int lit = 0;
        for (ServerLevel level : server.getAllLevels()) {
            // Only levels that actually have a player in them, so an empty dimension costs nothing.
            if (level.players().isEmpty()) {
                continue;
            }
            // One query per level rather than one per player: the radius check below covers
            // "near at least one player", which is the condition we actually want.
            for (Mob mob : level.getEntities(
                    EntityTypeTest.forClass(Mob.class),
                    searchBox(),
                    candidate -> candidate instanceof Enemy
                            && candidate.isAlive()
                            && !candidate.isRemoved()
                            && withinAnyPlayer(level, candidate))) {
                light(mob, now);
                lit++;
            }
        }

        // One line every few seconds is enough to prove the scan is finding mobs. Without it a
        // silent feature (no mobs in range) is indistinguishable from a broken one.
        if (lit > 0 && (now % (MobGlowTrackingModule.SCAN_INTERVAL * 6L)) < MobGlowTrackingModule.SCAN_INTERVAL) {
            // "in range" and "total lit" are different numbers by design: a mob keeps its
            // remaining time after walking out of range, so the second number is larger. A large
            // drop in it is the two-minute window expiring in bulk.
            int outOfRange = Math.max(0, LIT.size() - lit);
            TrackerMod.LOGGER.info("[tracker] mob glow: {} in range, {} total lit ({} out of range), outline active={}",
                    lit, LIT.size(), outOfRange, glowingVerified);
        }
    }

    /**
     * A single AABB that safely contains every possible 128-block sphere in the level. Building it
     * once and filtering in the predicate keeps the hot path to one query per level instead of one
     * per player; the predicate does the exact sphere test.
     */
    private static AABB searchBox() {
        double reach = MobGlowTrackingModule.RADIUS;
        return new AABB(
                -3.0E7 - reach, -2048.0, -3.0E7 - reach,
                3.0E7 + reach, 2048.0, 3.0E7 + reach);
    }

    private static boolean withinAnyPlayer(ServerLevel level, Mob mob) {
        double radiusSq = MobGlowTrackingModule.RADIUS * MobGlowTrackingModule.RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mob) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private static void light(Mob mob, long now) {
        UUID uuid = mob.getUUID();
        Long expiry = LIT.get(uuid);

        if (expiry != null && expiry > now) {
            // Still inside its two-minute window. Make sure the flag is actually on -- it can be
            // cleared by something else, and re-asserting it is free.
            if (!mob.hasGlowingTag()) {
                mob.setGlowingTag(true);
                OURS.add(uuid);
            }
            // isCurrentlyGlowing() also accounts for effects that suppress the outline, so it is
            // the honest check. Recorded once so the log can state it plainly.
            if (!glowingVerified && mob.isCurrentlyGlowing()) {
                glowingVerified = true;
            }
            return;
        }

        // Fresh (or just-expired) mob in range: start a new window.
        mob.setGlowingTag(true);
        OURS.add(uuid);
        LIT.put(uuid, now + MobGlowTrackingModule.HIGHLIGHT_TICKS);
        if (!glowingVerified && mob.isCurrentlyGlowing()) {
            glowingVerified = true;
        }
    }
}
