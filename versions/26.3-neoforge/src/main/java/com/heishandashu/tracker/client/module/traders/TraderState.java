package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.net.TraderNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the client knows about 流浪商人追踪: the traders the server told it about, and which ones the
 * player asked to highlight.
 */
public final class TraderState {

    /** Ten minutes, in client ticks. */
    public static final int HIGHLIGHT_TICKS = 12000;

    private static final Map<UUID, TraderNetworking.TraderSnap> TRADERS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> HIGHLIGHTS = new LinkedHashMap<>();

    private static int clientTicks;

    private TraderState() {
    }

    public static void accept(TraderNetworking.TraderListPayload payload) {
        for (TraderNetworking.TraderSnap snap : payload.traders()) {
            // The server marks the first sync after it noticed the trader, and only that one, so
            // the alert fires once and never for traders that already existed when the player
            // joined. Judging this from the countdown would be wrong: a spawn-egg trader keeps a
            // despawn delay of zero because only vanilla's own spawner sets it.
            if (snap.justDetected() && !TRADERS.containsKey(snap.uuid())) {
                TraderSounds.playSpawnAlert();
            }
        }

        TRADERS.clear();
        for (TraderNetworking.TraderSnap snap : payload.traders()) {
            TRADERS.put(snap.uuid(), snap);
        }
        // Highlights of traders that no longer exist are pointless.
        HIGHLIGHTS.keySet().removeIf(uuid -> !TRADERS.containsKey(uuid));
    }

    public static void tick() {
        clientTicks++;
        HIGHLIGHTS.entrySet().removeIf(entry -> clientTicks >= entry.getValue());
    }

    public static List<TraderNetworking.TraderSnap> traders() {
        return new ArrayList<>(TRADERS.values());
    }

    public static boolean highlight(UUID uuid) {
        if (!TRADERS.containsKey(uuid)) {
            return false;
        }
        HIGHLIGHTS.put(uuid, clientTicks + HIGHLIGHT_TICKS);
        return true;
    }

    public static boolean isHighlighted(UUID uuid) {
        return HIGHLIGHTS.containsKey(uuid);
    }

    public static int highlightRemaining(UUID uuid) {
        Integer until = HIGHLIGHTS.get(uuid);
        return until == null ? 0 : Math.max(0, until - clientTicks);
    }

    public static void clear() {
        TRADERS.clear();
        HIGHLIGHTS.clear();
    }

    /** The trader's live position when its entity is loaded here, otherwise the synced one. */
    public static Vec3 position(TraderNetworking.TraderSnap snap) {
        Entity entity = Minecraft.getInstance().level == null
                ? null : Minecraft.getInstance().level.getEntity(snap.uuid());
        if (entity != null) {
            return entity.position().add(0.0, entity.getBbHeight() + 0.4, 0.0);
        }
        return new Vec3(snap.x(), snap.y() + 2.4, snap.z());
    }

    /** Distance from the local player, or a very large number when there is no player. */
    public static double distance(TraderNetworking.TraderSnap snap) {
        if (Minecraft.getInstance().player == null) {
            return Double.MAX_VALUE;
        }
        return Minecraft.getInstance().player.position().distanceTo(position(snap));
    }

    /** mm:ss for a tick countdown. */
    public static String clock(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
