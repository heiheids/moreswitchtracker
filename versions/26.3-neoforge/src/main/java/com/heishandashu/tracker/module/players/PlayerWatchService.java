package com.heishandashu.tracker.module.players;

import com.heishandashu.tracker.compat.TrackerEvents;
import com.heishandashu.tracker.net.PlayerTrackNetworking;
import com.heishandashu.tracker.net.TrackerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server half of 玩家追踪: keeps each client's watch list and pushes those players' positions.
 *
 * <p>This exists because a client only has an entity for players near it. Asking the server is the
 * only way a highlight can stay pinned to someone on the other side of the world.</p>
 *
 * <p>NeoForge attaches the server-side handler to the channel at registration time instead of
 * letting the receiver register itself, so {@link #handleWatch} is handed to
 * {@link TrackerNetwork#serverbound} by the module that owns the channel.</p>
 */
public final class PlayerWatchService {

    /** How often positions are pushed (twice a second). */
    private static final int PUSH_INTERVAL = 10;

    /** watcher -&gt; watched. */
    private static final Map<UUID, Set<UUID>> WATCHERS = new HashMap<>();

    private static int timer;
    private static boolean initialized;

    private PlayerWatchService() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TrackerEvents.onServerTick(PlayerWatchService::serverTick);
    }

    /**
     * client -&gt; server: one client's new watch list.
     *
     * <p>Payload handling runs off the game thread, so the map update is hopped back onto it --
     * the same thing Fabric's {@code context.server().execute(...)} did.</p>
     */
    public static void handleWatch(PlayerTrackNetworking.WatchPayload payload, IPayloadContext context) {
        UUID watcher = context.player().getUUID();
        Set<UUID> targets = new HashSet<>(payload.targets());
        context.enqueueWork(() -> WATCHERS.put(watcher, targets));
    }

    private static void serverTick(MinecraftServer server) {
        if (WATCHERS.isEmpty() || !PlayerTrackingModule.isActive()) {
            return;
        }
        if (++timer < PUSH_INTERVAL) {
            return;
        }
        timer = 0;

        List<UUID> offline = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : WATCHERS.entrySet()) {
            ServerPlayer watcher = server.getPlayerList().getPlayer(entry.getKey());
            if (watcher == null) {
                offline.add(entry.getKey()); // left the game; forget them
                continue;
            }
            Set<UUID> targets = entry.getValue();
            if (targets.isEmpty()) {
                continue;
            }

            List<PlayerTrackNetworking.Position> positions = new ArrayList<>(targets.size());
            for (UUID targetId : targets) {
                ServerPlayer target = server.getPlayerList().getPlayer(targetId);
                if (target == null) {
                    continue; // not online right now
                }
                positions.add(new PlayerTrackNetworking.Position(targetId,
                        target.level().dimension().identifier().toString(),
                        target.getX(), target.getY(), target.getZ()));
            }
            if (!positions.isEmpty()) {
                TrackerNetwork.sendToPlayer(watcher, new PlayerTrackNetworking.PositionPayload(positions));
            }
        }
        offline.forEach(WATCHERS::remove);
    }
}
