package com.heishandashu.tracker.module.persistent;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.net.PickedMobNetworking;
import com.heishandashu.tracker.compat.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server half of 不消失怪物追踪: records every mob that picks a dropped item up.
 *
 * <p>A mob that picks up loot calls {@code setPersistenceRequired()}, so it will never despawn --
 * that is the thing worth finding. Detection is a hook on {@code Mob#pickUpItem}, which runs for
 * any mob anywhere on the server, not just around a player.</p>
 *
 * <p>Records are kept independent of chunks: a record's position is refreshed while its mob is
 * loaded and simply stays put once the chunk unloads, which is correct because an unloaded mob
 * cannot move. That is also why the client can be sent a fresh list once a second with no
 * client-to-server channel at all.</p>
 */
public final class PickedMobTracker {

    /** How often the list is pushed to clients (once a second). */
    private static final int SYNC_INTERVAL = 20;
    /** Upper bound on remembered mobs, oldest dropped first. */
    private static final int MAX_RECORDS = 512;
    /** Upper bound on entries sent per packet, newest first. */
    private static final int MAX_SYNCED = 64;

    private static final Map<UUID, PickedMobRecord> RECORDS = new LinkedHashMap<>();

    private static int nextId = 1;
    private static int syncTimer;
    private static boolean initialized;

    private PickedMobTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerEvents.onEndTick(PickedMobTracker::serverTick);
    }

    /** Called from the {@code Mob#pickUpItem} mixin. */
    public static void onPickup(Mob mob, ServerLevel level, ItemEntity item) {
        if (!PersistentMobTrackingModule.isActive()) {
            return;
        }
        ItemStack picked = item.getItem();
        if (picked.isEmpty()) {
            return;
        }

        PickedMobRecord existing = RECORDS.get(mob.getUUID());
        if (existing != null) {
            // Already known: newer loot, newer position, no second entry.
            existing.refreshStack(picked);
            existing.refresh(mob.blockPosition(), level.getGameTime());
            return;
        }

        if (nextId > 99999) {
            nextId = 1;
        }
        PickedMobRecord record = new PickedMobRecord(nextId++, mob, picked, level.getGameTime());
        RECORDS.put(record.uuid(), record);
        while (RECORDS.size() > MAX_RECORDS) {
            UUID oldest = RECORDS.keySet().iterator().next();
            RECORDS.remove(oldest);
        }

        TrackerMod.LOGGER.info("[tracker] picked-mob: {} {} took {} x{} at {} {}",
                record.label(), mob.getType().toShortString(), picked.getHoverName().getString(),
                picked.getCount(), record.dimension(), record.pos().toShortString());

        PickedMobReport.report(level.getServer(), sorted());
    }

    private static void serverTick(MinecraftServer server) {
        if (RECORDS.isEmpty() || !PersistentMobTrackingModule.isActive()) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;

        refreshPositions(server);
        broadcast(server);
    }

    /** Keeps each record's position current while its mob is loaded. */
    private static void refreshPositions(MinecraftServer server) {
        for (PickedMobRecord record : RECORDS.values()) {
            ServerLevel level = levelOf(server, record);
            if (level == null) {
                continue;
            }
            // Server side: search this level's entities directly. Naming the client-side
            // EntityLookup here would make a dedicated server fail to load this class at all.
            Mob mob = findByUuid(level, record.uuid());
            if (mob != null) {
                record.refresh(mob.blockPosition(), level.getGameTime());
            }
        }
    }

    /** The mob with this UUID in the given level, or null when it is not loaded. */
    private static Mob findByUuid(ServerLevel level, java.util.UUID uuid) {
        // A whole-level box is the only way to ask 1.21.1 for "an entity by uuid": the level has
        // no uuid index on the server side, only a network-id lookup.
        net.minecraft.world.phys.AABB everywhere = new net.minecraft.world.phys.AABB(
                -3.0E7, -2.0E4, -3.0E7, 3.0E7, 2.0E4, 3.0E7);
        java.util.List<Mob> matches = level.getEntitiesOfClass(Mob.class, everywhere,
                candidate -> candidate.getUUID().equals(uuid));
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static void broadcast(MinecraftServer server) {
        List<PickedMobNetworking.PickedMobSnap> snaps = new ArrayList<>();
        List<PickedMobRecord> ordered = sorted();
        for (int i = ordered.size() - 1; i >= 0 && snaps.size() < MAX_SYNCED; i--) {
            snaps.add(ordered.get(i).snapshot());
        }
        PickedMobNetworking.PickedMobListPayload payload =
                new PickedMobNetworking.PickedMobListPayload(snaps);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerEvents.send(player, payload);
        }
    }

    /** Records in display order (their ids). */
    public static List<PickedMobRecord> sorted() {
        List<PickedMobRecord> list = new ArrayList<>(RECORDS.values());
        list.sort(Comparator.comparingInt(PickedMobRecord::id));
        return list;
    }

    /** All records, non-capped, for reporting. */
    public static Collection<PickedMobRecord> all() {
        return List.copyOf(RECORDS.values());
    }

    private static ServerLevel levelOf(MinecraftServer server, PickedMobRecord record) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(record.dimension())) {
                return level;
            }
        }
        return null;
    }
}
