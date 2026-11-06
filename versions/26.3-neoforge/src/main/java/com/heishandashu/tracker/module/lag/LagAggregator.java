package com.heishandashu.tracker.module.lag;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rolling window aggregation for 卡顿来源追踪.
 *
 * <p>Samples are grouped by (dimension, chunk) and, inside a group, by the dominant source type.
 * The grouping key is a packed long and the type key uses identity semantics on the singleton
 * {@code EntityType}/{@code Identifier} instances, so <b>nothing is allocated while measuring</b>
 * -- that matters because this code runs once per entity per sampled tick.</p>
 *
 * <p>Kept free of Minecraft world access on purpose: it only receives numbers, which makes it
 * trivial to reason about and to test.</p>
 */
public final class LagAggregator {

    /**
     * How many ticks a window covers (30 seconds at 20 tps), i.e. {@code LagProfiler}'s
     * {@code SAMPLES_PER_WINDOW * SAMPLE_EVERY}. Kept here so the window length has one readable
     * definition next to the aggregation it describes.
     */
    public static final int WINDOW_TICKS = 600;

    /** One chunk's accumulated cost inside the current window. */
    private static final class Group {
        long nanos;
        int count;
        int lastX;
        int lastY;
        int lastZ;
        /** type instance -> [nanos, count] */
        final IdentityHashMap<Object, long[]> byType = new IdentityHashMap<>();

        void add(Object type, long nanos, int x, int y, int z) {
            this.nanos += nanos;
            this.count++;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            long[] slot = this.byType.get(type);
            if (slot == null) {
                slot = new long[2];
                this.byType.put(type, slot);
            }
            slot[0] += nanos;
            slot[1]++;
        }
    }

    /** One reported hot spot. */
    public record HotSpot(String kind, Object dimension, int chunkX, int chunkZ,
                          int x, int y, int z, Object type, int count, long nanos,
                          double millisPerTick, double shareOfAttributed) {
    }

    private static final Long2ObjectOpenHashMap<Group> ENTITIES = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Group> BLOCK_ENTITIES = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Group> CHUNKS = new Long2ObjectOpenHashMap<>();

    private static final IdentityHashMap<Object, Integer> DIMENSION_IDS = new IdentityHashMap<>();
    private static final List<Object> DIMENSIONS = new ArrayList<>();

    private static int sampledTicks;

    private LagAggregator() {
    }

    /** Interns a dimension so the map key stays primitive. Called once per measurement. */
    public static int dimensionId(Object dimension) {
        Integer id = DIMENSION_IDS.get(dimension);
        if (id != null) {
            return id;
        }
        int next = DIMENSIONS.size();
        DIMENSION_IDS.put(dimension, next);
        DIMENSIONS.add(dimension);
        return next;
    }

    public static Object dimension(int id) {
        return id >= 0 && id < DIMENSIONS.size() ? DIMENSIONS.get(id) : null;
    }

    private static long pack(int dimensionId, int chunkX, int chunkZ) {
        return ((long) dimensionId & 0xFFFFL) << 48
                | ((long) chunkX & 0xFFFFFFL) << 24
                | ((long) chunkZ & 0xFFFFFFL);
    }

    /** Records one measurement. {@code kind} is one of {@code entity}/{@code block_entity}/{@code chunk}. */
    public static void record(String kind, int dimensionId, int chunkX, int chunkZ,
                              Object type, long nanos, int x, int y, int z) {
        Long2ObjectOpenHashMap<Group> map = mapFor(kind);
        if (map == null) {
            return;
        }
        long key = pack(dimensionId, chunkX, chunkZ);
        Group group = map.get(key);
        if (group == null) {
            group = new Group();
            map.put(key, group);
        }
        group.add(type, nanos, x, y, z);
    }

    private static Long2ObjectOpenHashMap<Group> mapFor(String kind) {
        return switch (kind) {
            case "entity" -> ENTITIES;
            case "block_entity" -> BLOCK_ENTITIES;
            case "chunk" -> CHUNKS;
            default -> null;
        };
    }

    /** The measuring tick counter, advanced once per window. */
    public static void endWindow() {
        sampledTicks++;
    }

    /** Total nanoseconds attributed in the window, per kind. */
    public static long totalNanos() {
        long total = 0;
        for (Long2ObjectOpenHashMap<Group> map : List.of(ENTITIES, BLOCK_ENTITIES, CHUNKS)) {
            for (Group group : map.values()) {
                total += group.nanos;
            }
        }
        return total;
    }

    /** Top hot spots of the window, biggest first. */
    public static List<HotSpot> top(int limit) {
        long attributed = Math.max(1L, totalNanos());
        List<HotSpot> spots = new ArrayList<>();
        collect(spots, "entity", ENTITIES, attributed);
        collect(spots, "block_entity", BLOCK_ENTITIES, attributed);
        collect(spots, "chunk", CHUNKS, attributed);
        spots.sort(Comparator.comparingDouble(HotSpot::millisPerTick).reversed());
        return spots.size() <= limit ? spots : spots.subList(0, limit);
    }

    private static void collect(List<HotSpot> out, String kind, Long2ObjectOpenHashMap<Group> map,
                                long attributed) {
        // Each sample covers one whole tick, so dividing by the number of sampled ticks gives the
        // average per-tick cost of this group.
        double perTickDivisor = Math.max(1, LagProfiler.sampledTicks());
        for (Map.Entry<Long, Group> entry : map.entrySet()) {
            long key = entry.getKey();
            Group group = entry.getValue();
            out.add(new HotSpot(kind,
                    dimension((int) ((key >>> 48) & 0xFFFFL)),
                    (int) ((key >> 24) & 0xFFFFFFL),
                    (int) (key & 0xFFFFFFL),
                    group.lastX, group.lastY, group.lastZ,
                    dominantType(group),
                    group.count,
                    group.nanos,
                    group.nanos / 1_000_000.0 / perTickDivisor,
                    group.nanos * 100.0 / attributed));
        }
    }

    private static Object dominantType(Group group) {
        Object best = null;
        long bestNanos = -1;
        for (Map.Entry<Object, long[]> entry : group.byType.entrySet()) {
            if (entry.getValue()[0] > bestNanos) {
                bestNanos = entry.getValue()[0];
                best = entry.getKey();
            }
        }
        return best;
    }

    /** Clears the window; called after each report. */
    public static void reset() {
        ENTITIES.clear();
        BLOCK_ENTITIES.clear();
        CHUNKS.clear();
        sampledTicks = 0;
    }
}
