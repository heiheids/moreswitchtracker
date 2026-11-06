package com.heishandashu.tracker.client.module.lag;

import com.heishandashu.tracker.net.LagNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the client knows about 卡顿来源追踪: the hot spots the server last measured, and which ones
 * the player asked to highlight.
 *
 * <p>A hot spot is a place, not an entity, so a highlight is a fixed coordinate -- no syncing or
 * following is needed once the id has been resolved.</p>
 */
public final class LagState {

    /** Ten minutes, in client ticks. */
    public static final int HIGHLIGHT_TICKS = 12000;

    private static final Map<Integer, LagNetworking.LagSnap> SPOTS = new LinkedHashMap<>();
    private static final Map<Integer, Spot> HIGHLIGHTS = new LinkedHashMap<>();

    private static int clientTicks;

    private LagState() {
    }

    /** A highlighted hot spot; the data is copied so it survives later list updates. */
    public record Spot(int id, String name, String dimension, double x, double y, double z, int untilTick) {

        public int remaining() {
            return Math.max(0, this.untilTick - clientTicks);
        }
    }

    public static void accept(LagNetworking.LagListPayload payload) {
        SPOTS.clear();
        for (LagNetworking.LagSnap snap : payload.spots()) {
            SPOTS.put(snap.id(), snap);
        }
    }

    public static void tick() {
        clientTicks++;
        HIGHLIGHTS.entrySet().removeIf(entry -> clientTicks >= entry.getValue().untilTick());
    }

    public static List<LagNetworking.LagSnap> spots() {
        return new ArrayList<>(SPOTS.values());
    }

    public static List<Spot> highlights() {
        return new ArrayList<>(HIGHLIGHTS.values());
    }

    public static boolean hasSpots() {
        return !SPOTS.isEmpty();
    }

    /** Highlights a measured hot spot by its number. @return false when that id is unknown */
    public static boolean highlight(int id) {
        LagNetworking.LagSnap snap = SPOTS.get(id);
        if (snap == null) {
            return false;
        }
        HIGHLIGHTS.put(id, new Spot(id, displayName(snap), snap.dimension(),
                snap.x(), snap.y(), snap.z(), clientTicks + HIGHLIGHT_TICKS));
        return true;
    }

    public static void clear() {
        SPOTS.clear();
        HIGHLIGHTS.clear();
    }

    /** {@code #L0001} */
    public static String label(int id) {
        return String.format("#L%04d", id);
    }

    /** Kind label in the client's language. */
    public static Component kind(String kind) {
        return Component.translatable("tracker.lag.kind." + kind);
    }

    /** What was burning the time, named in the client's language. */
    public static String displayName(LagNetworking.LagSnap snap) {
        if ("entity".equals(snap.kind())) {
            Identifier id = Identifier.tryParse(snap.typeId());
            EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null) {
                return type.getDescription().getString();
            }
        }
        if ("chunk".equals(snap.kind())) {
            return kind("chunk").getString();
        }
        return snap.typeId();
    }

    /** Dimension name in the client's language. */
    public static String dimension(String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        if (id == null) {
            return dimension;
        }
        return Component.translatableWithFallback(
                "tracker.dimension." + id.getNamespace() + "." + id.getPath(), dimension).getString();
    }

    /** mm:ss. */
    public static String clock(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
