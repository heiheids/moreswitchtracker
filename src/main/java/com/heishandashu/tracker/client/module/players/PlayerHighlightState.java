package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.net.PlayerTrackNetworking;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who is currently being highlighted, for how much longer, and where they were last seen.
 *
 * <p>Client-side only. Positions arrive from the server ({@link PlayerTrackNetworking}) because a
 * player beyond the client's entity-sync range has no entity to follow -- without that, a far
 * highlight would silently disappear.</p>
 */
public final class PlayerHighlightState {

    /** Ten minutes. */
    public static final int HIGHLIGHT_TICKS = 12000;

    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();
    private static final Map<UUID, SyncedPosition> POSITIONS = new LinkedHashMap<>();

    private static int clientTicks;
    /** Set when the watch set changes, so the client module can tell the server once. */
    private static boolean watchDirty = true;

    private PlayerHighlightState() {
    }

    /** One highlighted player. */
    public record Entry(UUID uuid, String name, int untilTick) {

        public int remaining() {
            return Math.max(0, this.untilTick - clientTicks);
        }
    }

    /** Where the server last said a player was. */
    public record SyncedPosition(String dimension, Vec3 pos) {
    }

    /** Called once per client tick. */
    public static void tick() {
        clientTicks++;
        boolean changed = ACTIVE.entrySet().removeIf(entry -> clientTicks >= entry.getValue().untilTick());
        if (changed) {
            watchDirty = true;
        }
    }

    /** Starts (or restarts) a ten-minute highlight. */
    public static void highlight(UUID uuid, String name) {
        ACTIVE.put(uuid, new Entry(uuid, name, clientTicks + HIGHLIGHT_TICKS));
        watchDirty = true;
    }

    public static Collection<Entry> active() {
        return List.copyOf(ACTIVE.values());
    }

    public static void clear() {
        if (!ACTIVE.isEmpty()) {
            watchDirty = true;
        }
        ACTIVE.clear();
        POSITIONS.clear();
    }

    /** Applies a server position update. */
    public static void acceptPositions(PlayerTrackNetworking.PositionPayload payload) {
        for (PlayerTrackNetworking.Position position : payload.positions()) {
            POSITIONS.put(position.uuid(),
                    new SyncedPosition(position.dimension(), new Vec3(position.x(), position.y(), position.z())));
        }
    }

    /** Last position reported by the server, or null. */
    public static SyncedPosition position(UUID uuid) {
        return POSITIONS.get(uuid);
    }

    /** True once until the client has pushed its watch set to the server. */
    public static boolean watchDirty() {
        return watchDirty;
    }

    public static void watchSent() {
        watchDirty = false;
    }

    public static List<UUID> watchIds() {
        return new ArrayList<>(new LinkedHashSet<>(ACTIVE.keySet()));
    }

    /** Ticks left on a highlight, in client ticks. */
    public static int remaining(UUID uuid) {
        Entry entry = ACTIVE.get(uuid);
        return entry == null ? 0 : entry.remaining();
    }

    /** The server's position is only useful while the player is in the same dimension. */
    public static boolean sameDimension(SyncedPosition position, String clientDimension) {
        return position != null && position.dimension().equals(clientDimension);
    }

    /** Mutable set access for the watch sync. */
    public static Set<UUID> watchedIds() {
        return new LinkedHashSet<>(ACTIVE.keySet());
    }
}
