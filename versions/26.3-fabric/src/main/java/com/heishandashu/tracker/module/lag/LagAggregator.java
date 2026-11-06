package com.heishandashu.tracker.module.lag;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rolling window aggregation for 卡顿来源追踪.
 *
 * <p>Samples are grouped by (dimension, chunk) and, inside a group, by the source type's id.
 * The grouping key is a packed long and the type key is the registry id <b>string</b>, so nothing
 * is allocated for the chunk key and two samples of the same type always land in the same bucket.</p>
 *
 * <p>The type key used to be keyed by <em>identity</em> (an {@code EntityType} or block-entity
 * type instance). That was wrong: a block entity's {@code getType()} is not guaranteed to return
 * the same {@code String} instance every call, so the map gained a new entry per sample, grew
 * without bound, and eventually the report threw a {@link NullPointerException} out of the server
 * tick loop and crashed the game. Ids compare by value, so that cannot happen.</p>
 *
 * <p>Kept free of Minecraft world access on purpose: it only receives numbers and ids, which makes
 * it trivial to reason about and to test.</p>
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
        /** source type id -> [nanos, samples] */
        final HashMap<String, long[]> byType = new HashMap<>();

        void add(String typeId, long nanos, int x, int y, int z) {
            this.nanos += nanos;
            this.count++;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            long[] slot = this.byType.get(typeId);
            if (slot == null) {
                slot = new long[2];
                this.byType.put(typeId, slot);
            }
            slot[0] += nanos;
            slot[1]++;
        }
    }

    /** One reported hot spot. */
    public record HotSpot(String kind, Object dimension, int chunkX, int chunkZ,
                          int x, int y, int z, String typeId, int count, long nanos,
                          double millisPerTick, double shareOfAttributed) {
    }

    /** One source type's cost summed over every chunk in the window. */
    public record TypeTotal(String kind, String typeId, int count, double millisPerTick,
                            double shareOfAttributed) {
    }

    private static final Long2ObjectOpenHashMap<Group> ENTITIES = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Group> BLOCK_ENTITIES = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Group> CHUNKS = new Long2ObjectOpenHashMap<>();

    private static final IdentityHashMap<Object, Integer> DIMENSION_IDS = new IdentityHashMap<>();
    private static final List<Object> DIMENSIONS = new ArrayList<>();

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

    /**
     * Records one measurement.
     *
     * @param kind    one of {@code entity}/{@code block_entity}/{@code chunk}
     * @param typeId  the source type's registry id (e.g. {@code minecraft:zombie})
     */
    public static void record(String kind, int dimensionId, int chunkX, int chunkZ,
                              String typeId, long nanos, int x, int y, int z) {
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
        group.add(typeId == null ? "unknown" : typeId, nanos, x, y, z);
    }

    private static Long2ObjectOpenHashMap<Group> mapFor(String kind) {
        return switch (kind) {
            case "entity" -> ENTITIES;
            case "block_entity" -> BLOCK_ENTITIES;
            case "chunk" -> CHUNKS;
            default -> null;
        };
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
            Long key = entry.getKey();
            Group group = entry.getValue();
            if (key == null || group == null) {
                continue; // defensive: a half-cleared map must not take the server down
            }
            long packed = key;
            out.add(new HotSpot(kind,
                    dimension((int) ((packed >>> 48) & 0xFFFFL)),
                    (int) ((packed >> 24) & 0xFFFFFFL),
                    (int) (packed & 0xFFFFFFL),
                    group.lastX, group.lastY, group.lastZ,
                    dominantType(group),
                    concurrentCount(group, perTickDivisor),
                    group.nanos,
                    group.nanos / 1_000_000.0 / perTickDivisor,
                    group.nanos * 100.0 / attributed));
        }
    }

    /**
     * How many of these were present at once, which is what the player reads as "how many are
     * here".
     *
     * <p>{@link Group#count} counts <b>samples, not entities</b>: the same item entity is recorded
     * once per sampled tick, so a 120-sample window inflated the number 120x and a pile of 288
     * items was reported as {@code x34560}. Dividing by the number of sampled ticks turns it back
     * into the average concurrent count.</p>
     */
    private static int concurrentCount(Group group, double sampledTicks) {
        return Math.max(1, (int) Math.round(group.count / sampledTicks));
    }

    /** Total number of groups in the window, across all three kinds. Diagnostics only. */
    public static int groupCount() {
        return ENTITIES.size() + BLOCK_ENTITIES.size() + CHUNKS.size();
    }

    /**
     * Costs grouped by <b>source type</b> instead of by chunk.
     *
     * <p>The per-chunk view answers "which place is bad". When the cost is spread over hundreds of
     * chunks no single one stands out, and the answer becomes invisible -- a server at 22 ms/tick
     * with a 1 ms worst chunk looks healthy to a per-chunk ranking. Totalling by type shows what
     * the cost actually <em>is</em> ("all the dropped items cost 15 ms/tick"), which is the
     * actionable view for distributed lag.</p>
     */
    public static List<TypeTotal> topTypes(int limit) {
        HashMap<String, long[]> totals = new HashMap<>();
        accumulate(totals, "entity", ENTITIES);
        accumulate(totals, "block_entity", BLOCK_ENTITIES);
        accumulate(totals, "chunk", CHUNKS);

        long attributed = Math.max(1L, totalNanos());
        double divisor = Math.max(1, LagProfiler.sampledTicks());
        List<TypeTotal> out = new ArrayList<>(totals.size());
        for (Map.Entry<String, long[]> entry : totals.entrySet()) {
            String key = entry.getKey();
            long[] slot = entry.getValue();
            if (key == null || slot == null) {
                continue;
            }
            // The key carries the kind, so the same id in two kinds stays two rows.
            int split = key.indexOf('|');
            String kind = split < 0 ? "" : key.substring(0, split);
            String typeId = split < 0 ? key : key.substring(split + 1);
            out.add(new TypeTotal(kind, typeId,
                    Math.max(1, (int) Math.round(slot[1] / divisor)),
                    slot[0] / 1_000_000.0 / divisor,
                    slot[0] * 100.0 / attributed));
        }
        out.sort(Comparator.comparingDouble(TypeTotal::millisPerTick).reversed());
        return out.size() <= limit ? out : out.subList(0, limit);
    }

    private static void accumulate(HashMap<String, long[]> totals, String kind,
                                   Long2ObjectOpenHashMap<Group> map) {
        for (Group group : map.values()) {
            if (group == null) {
                continue;
            }
            for (Map.Entry<String, long[]> entry : group.byType.entrySet()) {
                long[] sample = entry.getValue();
                if (sample == null || entry.getKey() == null) {
                    continue; // defensive, see the class comment
                }
                String key = kind + '|' + entry.getKey();
                long[] slot = totals.get(key);
                if (slot == null) {
                    slot = new long[2];
                    totals.put(key, slot);
                }
                slot[0] += sample[0];
                slot[1] += sample[1];
            }
        }
    }

    private static String dominantType(Group group) {
        String best = null;
        long bestNanos = -1;
        for (Map.Entry<String, long[]> entry : group.byType.entrySet()) {
            long[] slot = entry.getValue();
            if (slot == null) {
                continue; // defensive, see the class comment
            }
            if (slot[0] > bestNanos) {
                bestNanos = slot[0];
                best = entry.getKey();
            }
        }
        return best == null ? "unknown" : best;
    }

    /** Clears the window; called after each report. */
    public static void reset() {
        ENTITIES.clear();
        BLOCK_ENTITIES.clear();
        CHUNKS.clear();
    }
}
