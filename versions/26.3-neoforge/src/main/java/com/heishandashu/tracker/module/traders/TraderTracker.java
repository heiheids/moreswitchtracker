package com.heishandashu.tracker.module.traders;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.compat.TrackerEvents;
import com.heishandashu.tracker.net.TrackerNetwork;
import com.heishandashu.tracker.net.TraderNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server half of 流浪商人追踪 / wandering trader tracking.
 *
 * <p>A wandering trader removes itself once its {@code despawnDelay} runs out (about forty
 * minutes, and it stops counting while a player is trading with it), so knowing when it arrives
 * -- and how long is left -- is what makes it catchable.</p>
 *
 * <p>Detection is Fabric's entity-load event, so no mixin is needed. The record is deliberately
 * kept when the chunk unloads: vanilla's own countdown does not tick while unloaded either, so the
 * last value stays correct.</p>
 */
public final class TraderTracker {

    /** How often the list is pushed to clients (once a second). */
    private static final int SYNC_INTERVAL = 20;
    /** Records older than this without being seen are dropped (the trader is long gone). */
    private static final int STALE_TICKS = 20 * 60 * 60;

    private static final Map<UUID, TraderRecord> TRADERS = new LinkedHashMap<>();

    private static int syncTimer;
    private static boolean initialized;

    private TraderTracker() {
    }

    /** One tracked trader. Position and countdown are refreshed while it is loaded. */
    private static final class TraderRecord {
        final UUID uuid;
        final String dimension;
        double x;
        double y;
        double z;
        int despawnTicks;
        long lastSeen;
        /** Cleared once a sync carrying it has reached a player, so the alert fires exactly once. */
        boolean justDetected = true;

        TraderRecord(WanderingTrader trader) {
            this.uuid = trader.getUUID();
            this.dimension = trader.level().dimension().identifier().toString();
            this.x = trader.getX();
            this.y = trader.getY();
            this.z = trader.getZ();
            this.despawnTicks = trader.getDespawnDelay();
        }
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // NeoForge's EntityJoinLevelEvent hands over a Level; the trader scan only means anything
        // on a server level, so the cast happens at this one boundary.
        TrackerEvents.onEntityJoinLevel((entity, level) -> {
            if (level instanceof ServerLevel serverLevel) {
                onEntityLoaded(entity, serverLevel);
            }
        });
        TrackerEvents.onServerTick(TraderTracker::serverTick);
    }

    private static void onEntityLoaded(Entity entity, ServerLevel level) {
        if (!WanderingTraderTrackingModule.isActive() || !(entity instanceof WanderingTrader trader)) {
            return;
        }
        if (TRADERS.containsKey(trader.getUUID())) {
            return; // already announced this visit
        }
        TraderRecord record = new TraderRecord(trader);
        record.lastSeen = level.getGameTime();
        TRADERS.put(record.uuid, record);

        TrackerMod.LOGGER.info("[tracker] trader: wandering trader appeared at {} {}",
                record.dimension, trader.blockPosition().toShortString());
        announce(level.getServer(), record);
    }

    private static void serverTick(MinecraftServer server) {
        if (TRADERS.isEmpty() || !WanderingTraderTrackingModule.isActive()) {
            return;
        }
        if (++syncTimer < SYNC_INTERVAL) {
            return;
        }
        syncTimer = 0;

        long now = server.overworld() == null ? 0L : server.overworld().getGameTime();
        List<UUID> gone = new ArrayList<>();
        List<TraderNetworking.TraderSnap> snaps = new ArrayList<>(TRADERS.size());

        for (TraderRecord record : TRADERS.values()) {
            ServerLevel level = levelOf(server, record.dimension);
            if (level == null) {
                continue;
            }
            Entity entity = level.getEntity(record.uuid);
            if (entity instanceof WanderingTrader trader) {
                record.x = trader.getX();
                record.y = trader.getY();
                record.z = trader.getZ();
                record.despawnTicks = trader.getDespawnDelay();
                record.lastSeen = level.getGameTime();
            } else if (now - record.lastSeen > STALE_TICKS) {
                gone.add(record.uuid); // unloaded for over an hour of game time: forget it
                continue;
            }
            snaps.add(new TraderNetworking.TraderSnap(record.uuid, record.dimension,
                    record.x, record.y, record.z, record.despawnTicks, record.justDetected));
        }
        gone.forEach(TRADERS::remove);

        TraderNetworking.TraderListPayload payload = new TraderNetworking.TraderListPayload(snaps);
        boolean delivered = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TrackerNetwork.sendToPlayer(player, payload);
            delivered = true;
        }
        if (delivered) {
            // The arrival alert is a one-shot: hand it over, then stop advertising it.
            TRADERS.values().forEach(record -> record.justDetected = false);
        }
    }

    /** Chat announcement with a clickable coordinate that starts the ten-minute highlight. */
    private static void announce(MinecraftServer server, TraderRecord record) {
        if (server == null) {
            return;
        }
        MutableComponent coordinates = Component.literal("[" + (int) record.x + ", "
                        + (int) record.y + ", " + (int) record.z + "]")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/tracker trader " + record.uuid))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("tracker.trader.click_hint"))));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.translatable("tracker.trader.spawned", coordinates));
            player.sendSystemMessage(Component.translatable("tracker.trader.spawned_hint")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static ServerLevel levelOf(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(dimension)) {
                return level;
            }
        }
        return null;
    }
}
