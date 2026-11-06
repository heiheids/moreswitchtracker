package com.heishandashu.tracker.module.legacy;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.net.DtpNetworking;
import com.heishandashu.tracker.compat.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 遗产追踪 / Legacy Tracking -- the server-side half.
 *
 * <p>Server-authoritative on purpose: an item's age <em>is</em> the despawn timer, and that
 * number never reaches the client, so only the server can count down correctly. It also means
 * the countdown freezes by itself whenever the chunk unloads, because the entity stops
 * ticking -- which is exactly the behaviour that was asked for.</p>
 *
 * <p>Every entry point checks {@link LegacyTrackingModule#isActive()} first, so the switch
 * really does turn the whole feature off.</p>
 */
public final class LegacyTracker {

    /** Vanilla item lifetime: 6000 ticks -- five minutes of ticking time. */
    public static final int DESPAWN_TICKS = 6000;
    /** How long after a death we keep picking up freshly dropped items. */
    private static final int COLLECT_TICKS = 40;
    /** How often the countdown is pushed to clients (once a second). */
    private static final int SYNC_INTERVAL = 20;
    /** Only drops within this distance of the death are considered part of it. */
    private static final double COLLECT_RADIUS = 6.0;
    /**
     * How long a death point stays known after its five-minute window ends, in ticks of loaded
     * time. The countdown is over long before this, but the player may still be walking back, so
     * the chat coordinate has to keep working and the marker must still be highlightable.
     */
    public static final int RETENTION_TICKS = 36000; // 30 minutes of time in the area

    private static final Map<String, DeathRecord> DEATHS = new LinkedHashMap<>();
    private static final Map<UUID, TrackedDrop> DROPS = new HashMap<>();
    private static int nextId = 1;
    private static int syncTimer;
    /** True while clients hold a snapshot, so the "death list is empty" case is sent exactly once. */
    private static boolean syncSentSomething;
    private static boolean initialized;
    private static MinecraftServer currentServer;

    private LegacyTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerEvents.onLivingDeath(LegacyTracker::afterDeath);
        ServerEvents.onEndTick(LegacyTracker::serverTick);
    }

    // ------------------------------------------------------------------ death

    private static void afterDeath(LivingEntity entity, DamageSource source, ServerLevel level) {
        if (!LegacyTrackingModule.isActive() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        // The caller already resolved the level; reuse it instead of re-testing the player's.
        String id = "D" + nextId++;
        ResourceLocation dimension = level.dimension().location();
        BlockPos pos = player.blockPosition();
        Component cause = DamageCauseNames.shortName(source);
        DeathRecord record = new DeathRecord(id, player.getUUID(), player.getName().getString(),
                dimension, pos, level.getGameTime(), cause, COLLECT_TICKS);
        DEATHS.put(id, record);

        // 1) what killed them, 2) a clickable coordinate that starts the highlight
        MutableComponent coordinates =
                Component.literal("[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                        .withStyle(Style.EMPTY
                                .withColor(0x55FF55)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tracker show " + id))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                        Component.translatable("tracker.legacy.death.click_hint"))));

        player.sendSystemMessage(Component.translatable("tracker.legacy.death.header", cause));
        player.sendSystemMessage(Component.translatable("tracker.legacy.death.coords", coordinates));
        TrackerMod.LOGGER.info("[tracker] legacy: death {} in {} at {} tracked", id, dimension, pos);
    }

    // ------------------------------------------------------------------ ticking

    private static void serverTick(MinecraftServer server) {
        currentServer = server;
        boolean active = LegacyTrackingModule.isActive();

        if (!DEATHS.isEmpty()) {
            Iterator<Map.Entry<String, DeathRecord>> it = DEATHS.entrySet().iterator();
            while (it.hasNext()) {
                DeathRecord record = it.next().getValue();
                ServerLevel level = levelOf(server, record.dimension());
                if (level == null) {
                    it.remove();
                    clearDrops(record);
                    continue;
                }

                if (active) {
                    if (level.getGameTime() <= record.collectUntilGameTime()) {
                        collectDrops(level, record);
                    }
                    updateDrops(level, record);

                    // Nothing was dropped at all (keepInventory, or an empty inventory).
                    if (!record.announced() && record.drops().isEmpty()
                            && level.getGameTime() > record.collectUntilGameTime()) {
                        record.announced(true);
                        ServerPlayer owner = ownerOf(record);
                        if (owner != null) {
                            owner.sendSystemMessage(Component.translatable("tracker.legacy.death.no_drops"));
                        }
                    }
                }

                // The five-minute window is over and every drop is accounted for: print the
                // expiry list once. The record itself stays, so the chat coordinate keeps
                // working while the player walks back to the spot.
                boolean itemsSettled = record.allDropsGone()
                        || (record.drops().isEmpty() && record.announced());
                if (itemsSettled && !record.finished()) {
                    if (active) {
                        printFinalSummary(record);
                    }
                    record.finished(true);
                }

                // Only the retention clock actually retires the death point.
                if (record.retentionTicks() <= 0) {
                    it.remove();
                    clearDrops(record);
                }
            }
        }

        // The last death is gone: tell the client right away. Waiting for the periodic sync
        // would never happen, because the next tick returns early on an empty list -- that left
        // the countdown frozen on screen at 0:00.
        if (DEATHS.isEmpty()) {
            if (syncSentSomething) {
                syncToClients(server);
                syncSentSomething = false;
            }
            return;
        }

        if (active && ++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            syncToClients(server);
        }
    }

    /** Picks up the items the death just threw on the ground. */
    private static void collectDrops(ServerLevel level, DeathRecord record) {
        BlockPos pos = record.pos();
        AABB area = new AABB(pos).inflate(COLLECT_RADIUS);
        UUID ownerUuid = record.playerUuid();

        List<ItemEntity> fresh = level.getEntitiesOfClass(ItemEntity.class, area, item -> {
            if (item.getAge() > COLLECT_TICKS || DROPS.containsKey(item.getUUID())) {
                return false;
            }
            // Death loot normally has no thrower at all (vanilla only sets it when the stack
            // is thrown on purpose), so "no owner" must count as ours -- requiring an owner
            // here silently collected nothing and froze the countdown at 5:00.
            Entity owner = item.getOwner();
            return owner == null || owner.getUUID().equals(ownerUuid);
        });

        for (ItemEntity item : fresh) {
            TrackedDrop drop = new TrackedDrop(item.getUUID(), item.getItem());
            drop.entityId(item.getId());
            drop.entity(item);
            record.addDrop(drop);
            DROPS.put(item.getUUID(), drop);
        }
        if (!fresh.isEmpty()) {
            TrackerMod.LOGGER.info("[tracker] legacy: death {} tracked {} more drop(s), {} in total",
                    record.id(), fresh.size(), record.drops().size());
        }
    }

    /**
     * Advances the death's highlight window and refreshes every drop.
     *
     * <p>The window is the five minutes the death point stays clickable and highlighted. It is
     * counted down here, once per tick, only while the death area is actually loaded -- so an
     * unloaded chunk pauses it, exactly like the items it sits next to. Items never shorten the
     * window: losing them early is normal (lava, pickups) and the player may still want to walk
     * back to the spot.</p>
     */
    private static void updateDrops(ServerLevel level, DeathRecord record) {
        int remaining = record.remainingTicks();
        boolean loaded = level.isLoaded(record.pos());
        if (loaded) {
            remaining = Math.max(0, remaining - 1);
            record.retentionTicks(Math.max(0, record.retentionTicks() - 1));
        }

        Map<UUID, ItemEntity> nearby = null; // built once, only if some drop needs looking up

        for (TrackedDrop drop : record.dropList()) {
            if (!drop.stillInWorld()) {
                continue;
            }

            // Prefer the entity instance we already hold: it gives the real age without any
            // lookup, and isRemoved() tells us the chunk unloaded.
            ItemEntity item = drop.entity();
            if (item == null || item.isRemoved()) {
                item = null;
                // Server side: walk the level's own entity list. The client-side EntityLookup
                // helper must not be named here -- it lives under client/ and a dedicated server
                // would fail to load this class at all.
                ItemEntity foundItem = findItemByUuid(level, drop.uuid());
                if (foundItem != null) {
                    item = foundItem;
                } else {
                    // Area scan around the death point: this uses the same call that already
                    // collects the drops, so it does not depend on the uuid lookup working.
                    if (nearby == null) {
                        nearby = scanNear(level, record);
                    }
                    item = nearby.get(drop.uuid());
                }
            }

            if (item != null) {
                drop.entity(item);
                drop.entityId(item.getId());
                drop.state(TrackedDrop.State.ALIVE);
                drop.lastAge(item.getAge());
                // Deliberately NOT clamped by the item's age: the window is the death point's
                // own five minutes. Clamping pulled it early whenever an older stack (one the
                // player had dropped before dying, or a merged pile) was part of the death.
            } else {
                // Not in the level: the chunk unloaded (the item is saved with it and its age
                // is frozen), or the removal hook has not classified it yet.
                drop.state(TrackedDrop.State.PAUSED);
            }
        }

        record.remainingTicks(Math.max(0, remaining));
    }

    /** All item entities still near the death point, keyed by uuid. */
    private static Map<UUID, ItemEntity> scanNear(ServerLevel level, DeathRecord record) {
        AABB area = new AABB(record.pos()).inflate(COLLECT_RADIUS + 4.0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area, item -> true);
        Map<UUID, ItemEntity> byUuid = new HashMap<>(items.size() * 2);
        for (ItemEntity item : items) {
            byUuid.put(item.getUUID(), item);
        }
        return byUuid;
    }

    /**
     * The dropped item with this uuid anywhere in the level, or null.
     *
     * <p>1.21.1 has no uuid index on the server side, so this is a level-wide query. It only runs
     * for a drop whose entity reference was lost (chunk reload), never per tick for every drop.</p>
     */
    private static ItemEntity findItemByUuid(ServerLevel level, UUID uuid) {
        AABB everywhere = new AABB(-3.0E7, -2.0E4, -3.0E7, 3.0E7, 2.0E4, 3.0E7);
        List<ItemEntity> matches = level.getEntitiesOfClass(ItemEntity.class, everywhere,
                candidate -> candidate.getUUID().equals(uuid));
        return matches.isEmpty() ? null : matches.get(0);
    }

    // ------------------------------------------------------------------ hooks (called from mixins)

    /** An item's health ran out: remember what did it, so the chat line can name it. */
    public static void onDropDamaged(ItemEntity item, DamageSource source) {
        TrackedDrop drop = DROPS.get(item.getUUID());
        if (drop != null && drop.stillInWorld()) {
            drop.detail(DamageCauseNames.shortName(source));
        }
    }

    public static void onDropTakenByMob(ItemEntity item, Entity mob) {
        TrackedDrop drop = DROPS.get(item.getUUID());
        if (drop == null || !drop.stillInWorld()) {
            return;
        }
        drop.state(TrackedDrop.State.TAKEN_BY_MOB);
        drop.detail(DamageCauseNames.pickerName(mob));
        report(recordOf(drop), drop, "tracker.legacy.drop.taken_by_mob");
    }

    public static void onDropTakenByPlayer(ItemEntity item, Player player) {
        TrackedDrop drop = DROPS.get(item.getUUID());
        if (drop == null || !drop.stillInWorld()) {
            return;
        }
        drop.state(TrackedDrop.State.TAKEN_BY_PLAYER);
        drop.detail(player.getDisplayName());
        report(recordOf(drop), drop, "tracker.legacy.drop.taken_by_player");
    }

    /**
     * Two stacks tried to merge. Called after vanilla resolved it, so the absorbed one is the one
     * that is now removed -- deciding this by {@code isRemoved()} instead of by argument order is
     * what keeps the surviving stack tracked.
     */
    public static void onMergeResolved(ItemEntity self, ItemEntity other) {
        if (self.isRemoved() == other.isRemoved()) {
            return; // nothing merged
        }
        ItemEntity absorbed = self.isRemoved() ? self : other;
        ItemEntity survivor = self.isRemoved() ? other : self;

        TrackedDrop drop = DROPS.get(absorbed.getUUID());
        if (drop == null || !drop.stillInWorld()) {
            return;
        }
        drop.state(TrackedDrop.State.MERGED);
        drop.detail(survivor.getItem().getHoverName());
        DROPS.remove(absorbed.getUUID());
        TrackerMod.LOGGER.info("[tracker] legacy: drop {} x{} merged into {} (tracking follows the survivor)",
                drop.stack().getHoverName().getString(), drop.stack().getCount(),
                survivor.getItem().getHoverName().getString());
    }

    /**
     * The central classifier, fed by the {@code Entity#remove} mixin with the real removal
     * reason -- nothing here is guessed from geometry.
     */
    public static void onDropRemoved(ItemEntity item, Entity.RemovalReason reason) {
        TrackedDrop drop = DROPS.get(item.getUUID());
        if (drop == null || !drop.stillInWorld()) {
            return;
        }
        DeathRecord record = recordOf(drop);
        // Read the age straight off the entity being removed: the per-tick snapshot can be stale,
        // and a stale age is exactly what turned "expired after 5 minutes" into "unknown reason".
        int age = Math.max(item.getAge(), drop.lastAge());

        switch (reason) {
            case UNLOADED_TO_CHUNK -> {
                // Saved into the chunk: freeze, do not end.
                drop.state(TrackedDrop.State.PAUSED);
                return;
            }
            case UNLOADED_WITH_PLAYER -> drop.state(TrackedDrop.State.UNKNOWN);
            case KILLED -> drop.state(drop.detail() != null
                    ? TrackedDrop.State.DESTROYED : TrackedDrop.State.UNKNOWN);
            case DISCARDED, CHANGED_DIMENSION -> {
                if (drop.state() == TrackedDrop.State.TAKEN_BY_MOB
                        || drop.state() == TrackedDrop.State.TAKEN_BY_PLAYER
                        || drop.state() == TrackedDrop.State.MERGED) {
                    DROPS.remove(item.getUUID());
                    return; // already reported by the pickup/merge hook
                }
                if (drop.detail() != null) {
                    // Something damaged it and named itself: that beats the age check.
                    drop.state(TrackedDrop.State.DESTROYED);
                } else if (age >= DESPAWN_TICKS - 20) {
                    drop.state(TrackedDrop.State.EXPIRED);
                } else {
                    drop.state(TrackedDrop.State.UNKNOWN);
                }
            }
            default -> drop.state(TrackedDrop.State.UNKNOWN);
        }

        TrackerMod.LOGGER.info("[tracker] legacy: drop {} x{} -> {} (reason={}, age={}, detail={})",
                drop.stack().getHoverName().getString(), drop.stack().getCount(), drop.state(),
                reason, age, drop.detail() == null ? "-" : drop.detail().getString());

        if (drop.state() == TrackedDrop.State.DESTROYED) {
            report(record, drop, "tracker.legacy.drop.destroyed");
        } else if (drop.state() == TrackedDrop.State.UNKNOWN) {
            report(record, drop, "tracker.legacy.drop.vanished");
        }
        DROPS.remove(item.getUUID());
    }

    // ------------------------------------------------------------------ chat

    private static void report(DeathRecord record, TrackedDrop drop, String key) {
        if (record == null || drop.reported()) {
            return;
        }
        drop.reported(true);
        ServerPlayer owner = ownerOf(record);
        if (owner == null) {
            return;
        }
        ItemStack stack = drop.stack();
        Component detail = drop.detail() == null
                ? Component.translatable("tracker.cause.unknown") : drop.detail();
        owner.sendSystemMessage(Component.translatable(key, detail, stack.getHoverName(), stack.getCount()));
    }

    /** "These items are gone for good": printed once the death is over. */
    private static void printFinalSummary(DeathRecord record) {
        ServerPlayer owner = ownerOf(record);
        if (owner == null) {
            return;
        }
        List<TrackedDrop> expired = new ArrayList<>();
        for (TrackedDrop drop : record.dropList()) {
            if (drop.state() == TrackedDrop.State.EXPIRED) {
                expired.add(drop);
            }
        }

        // Always log the tally: if the player sees no list, this line says exactly why.
        TrackerMod.LOGGER.info("[tracker] legacy: death {} finished -- {} drop(s): {} expired, {} destroyed,"
                        + " {} taken, {} merged, {} unknown, {} alive, {} paused (chunk unloaded)",
                record.id(), record.drops().size(), expired.size(),
                count(record, TrackedDrop.State.DESTROYED), count(record, TrackedDrop.State.TAKEN_BY_MOB)
                        + count(record, TrackedDrop.State.TAKEN_BY_PLAYER),
                count(record, TrackedDrop.State.MERGED), count(record, TrackedDrop.State.UNKNOWN),
                count(record, TrackedDrop.State.ALIVE), count(record, TrackedDrop.State.PAUSED));

        if (expired.isEmpty()) {
            return;
        }
        owner.sendSystemMessage(Component.translatable("tracker.legacy.expired.header"));
        for (TrackedDrop drop : expired) {
            ItemStack stack = drop.stack();
            owner.sendSystemMessage(Component.translatable("tracker.legacy.expired.line",
                    stack.getHoverName(), stack.getCount()));
        }
    }

    private static int count(DeathRecord record, TrackedDrop.State state) {
        int n = 0;
        for (TrackedDrop drop : record.dropList()) {
            if (drop.state() == state) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ sync

    private static void syncToClients(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            List<DtpNetworking.DeathSnap> snaps = new ArrayList<>();
            for (DeathRecord record : DEATHS.values()) {
                if (!record.dimension().equals(level.dimension().location())) {
                    continue;
                }
                List<UUID> alive = new ArrayList<>();
                for (TrackedDrop drop : record.dropList()) {
                    if (drop.stillInWorld()) {
                        alive.add(drop.uuid());
                    }
                }
                snaps.add(new DtpNetworking.DeathSnap(record.id(),
                        record.dimension().toString(),
                        record.pos().getX(), record.pos().getY(), record.pos().getZ(),
                        record.remainingTicks(), alive));
            }
            // Send even when empty: that is what tells the client the death is over, so its
            // highlight and countdown stop instead of hanging on a stale snapshot.
            DtpNetworking.LegacySyncPayload payload = new DtpNetworking.LegacySyncPayload(snaps);
            if (!snaps.isEmpty()) {
                syncSentSomething = true;
            }
            for (ServerPlayer player : level.players()) {
                ServerEvents.send(player, payload);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static ServerLevel levelOf(MinecraftServer server, ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) {
                return level;
            }
        }
        return null;
    }

    private static ServerPlayer ownerOf(DeathRecord record) {
        return currentServer == null ? null : currentServer.getPlayerList().getPlayer(record.playerUuid());
    }

    private static DeathRecord recordOf(TrackedDrop drop) {
        for (DeathRecord record : DEATHS.values()) {
            if (record.drops().containsKey(drop.uuid())) {
                return record;
            }
        }
        return null;
    }

    private static void clearDrops(DeathRecord record) {
        for (UUID uuid : record.drops().keySet()) {
            DROPS.remove(uuid);
        }
        record.drops().clear();
    }
}
