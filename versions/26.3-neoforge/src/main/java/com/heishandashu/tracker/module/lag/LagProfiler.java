package com.heishandashu.tracker.module.lag;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.compat.TrackerEvents;
import com.heishandashu.tracker.net.LagNetworking;
import com.heishandashu.tracker.net.TrackerNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Times the server's own tick work and reports the biggest consumers.
 *
 * <p>Three measurements, all at points vanilla already calls one item at a time:</p>
 * <ul>
 *   <li>every non-passenger entity -- {@code ServerLevel#tickNonPassenger}</li>
 *   <li>every ticking block entity -- {@code LevelChunk$BoundTickingBlockEntity#tick}</li>
 *   <li>every chunk -- {@code ServerLevel#tickChunk} (random ticks, spawning, precipitation)</li>
 * </ul>
 *
 * <p>Only every {@link #SAMPLE_EVERY}-th tick is measured, which cuts the overhead by that factor
 * while leaving the ranking intact. The absolute numbers include vanilla's profiler push/pop, so
 * treat them as a ranking and a share, not as gospel milliseconds.</p>
 */
public final class LagProfiler {

    /** Measure one tick in this many. */
    private static final int SAMPLE_EVERY = 5;
    /**
     * Sampled ticks per window: 120 samples x 5 ticks = 600 ticks = 30 seconds. The window length
     * is the delay before the first report and the gap between reports; measurements are averages
     * per sampled tick, so {@link #ALERT_MILLIS_PER_TICK} means the same thing at any window
     * length. A shorter window reacts sooner but smooths out fewer one-off spikes, so the alert
     * can speak up more often.
     */
    private static final int SAMPLES_PER_WINDOW = 120;
    /** Only speak up when the worst group costs at least this much. */
    private static final double ALERT_MILLIS_PER_TICK = 5.0;
    /** How many entries a report shows. */
    private static final int REPORT_LIMIT = 5;
    /** A single measurement longer than this is treated as a stall and ignored. */
    private static final long MAX_MEASUREMENT_NANOS = 500_000_000L;

    /** Hot-spot key to the number the player sees, so the same offender keeps its {@code #L} id. */
    private static final Map<String, Integer> SPOT_IDS = new HashMap<>();
    private static int nextSpotId = 1;

    // Separate start slots per kind: an entity tick can happen while a chunk is being ticked, and
    // sharing one slot would corrupt both.
    private static long entityStart;
    private static long blockEntityStart;
    private static long chunkStart;

    private static int tickCounter;
    private static int sampledTicks;
    private static boolean sampleThisTick;
    private static boolean initialized;

    private LagProfiler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        TrackerEvents.onServerTickPre(server -> beginTick());
        TrackerEvents.onServerTick(LagProfiler::endTick);
    }

    /** Number of ticks actually measured in the current window; the divisor for ms/tick. */
    public static int sampledTicks() {
        return sampledTicks;
    }

    // ------------------------------------------------------------------ lifecycle

    private static void beginTick() {
        sampleThisTick = LagSourceTrackingModule.isActive() && tickCounter++ % SAMPLE_EVERY == 0;
        // Stale starts (an exception inside a measured call) must never leak into the next tick.
        entityStart = 0L;
        blockEntityStart = 0L;
        chunkStart = 0L;
    }

    private static void endTick(MinecraftServer server) {
        if (sampleThisTick) {
            sampledTicks++;
        }
        if (sampledTicks < SAMPLES_PER_WINDOW) {
            return;
        }
        if (LagSourceTrackingModule.isActive()) {
            report(server);
        }
        LagAggregator.reset();
        sampledTicks = 0;
    }

    // ------------------------------------------------------------------ measurements

    public static void entityStart() {
        if (sampleThisTick) {
            entityStart = System.nanoTime();
        }
    }

    public static void entityEnd(ServerLevel level, Entity entity) {
        if (!sampleThisTick || entityStart == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - entityStart;
        entityStart = 0L;
        if (elapsed <= 0L || elapsed > MAX_MEASUREMENT_NANOS) {
            return;
        }
        BlockPos pos = entity.blockPosition();
        LagAggregator.record("entity", dimensionId(level), pos.getX() >> 4, pos.getZ() >> 4,
                entity.getType(), elapsed, pos.getX(), pos.getY(), pos.getZ());
    }

    public static void blockEntityStart() {
        if (sampleThisTick) {
            blockEntityStart = System.nanoTime();
        }
    }

    public static void blockEntityEnd(TickingBlockEntity ticking, net.minecraft.world.level.Level level) {
        if (!sampleThisTick || blockEntityStart == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - blockEntityStart;
        blockEntityStart = 0L;
        if (elapsed <= 0L || elapsed > MAX_MEASUREMENT_NANOS || level == null) {
            return;
        }
        BlockPos pos = ticking.getPos();
        // Block entities carry their type as a string id, which is enough to find them again.
        LagAggregator.record("block_entity", LagAggregator.dimensionId(level.dimension().identifier()),
                pos.getX() >> 4, pos.getZ() >> 4, ticking.getType(), elapsed,
                pos.getX(), pos.getY(), pos.getZ());
    }

    public static void chunkStart() {
        if (sampleThisTick) {
            chunkStart = System.nanoTime();
        }
    }

    public static void chunkEnd(ServerLevel level, LevelChunk chunk) {
        if (!sampleThisTick || chunkStart == 0L) {
            return;
        }
        long elapsed = System.nanoTime() - chunkStart;
        chunkStart = 0L;
        if (elapsed <= 0L || elapsed > MAX_MEASUREMENT_NANOS) {
            return;
        }
        BlockPos pos = chunk.getPos().getWorldPosition();
        // ChunkPos.x/z are private in 26.3, so the chunk coordinates come from the block position.
        LagAggregator.record("chunk", dimensionId(level), pos.getX() >> 4, pos.getZ() >> 4,
                "chunk", elapsed, pos.getX(), pos.getY(), pos.getZ());
    }

    // ------------------------------------------------------------------ reporting

    private static void report(MinecraftServer server) {
        List<LagAggregator.HotSpot> top = LagAggregator.top(REPORT_LIMIT);
        if (top.isEmpty()) {
            return;
        }

        // Stable ids: the same hot spot keeps its number across windows, so a #L id the player
        // read in chat still means the same place a minute later.
        List<LagNetworking.LagSnap> snaps = new ArrayList<>(top.size());
        for (LagAggregator.HotSpot spot : top) {
            int id = idFor(spot);
            snaps.add(new LagNetworking.LagSnap(id, spot.kind(), typeIdOf(spot.type()), spot.count(),
                    String.valueOf(spot.dimension()),
                    spot.x(), spot.y(), spot.z(), spot.millisPerTick(), spot.shareOfAttributed()));
        }

        // Always push the list, even when nothing is bad enough to announce: the manual query and
        // the #L highlight both need current data.
        LagNetworking.LagListPayload payload = new LagNetworking.LagListPayload(snaps);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TrackerNetwork.sendToPlayer(player, payload);
        }

        if (top.get(0).millisPerTick() < ALERT_MILLIS_PER_TICK) {
            return; // quiet unless something is genuinely burning tick time
        }

        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        Component header = Component.translatable("tracker.lag.header",
                Integer.toString(SAMPLES_PER_WINDOW * SAMPLE_EVERY / 20),
                String.format("%.1f", mspt));
        Component hint = Component.translatable("tracker.lag.hint_manual").withStyle(ChatFormatting.GRAY);

        TrackerMod.LOGGER.info("[tracker] lag: worst {} ({}) at {} {} {} -- {} ms/tick",
                describe(top.get(0).type()), top.get(0).kind(),
                top.get(0).dimension(), top.get(0).x() + "," + top.get(0).y() + "," + top.get(0).z(),
                String.format("%.2f", top.get(0).millisPerTick()));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(header);
            for (int i = 0; i < top.size(); i++) {
                player.sendSystemMessage(line(top.get(i), snaps.get(i).id()));
            }
            player.sendSystemMessage(hint);
        }
    }

    /** Ids keyed by what the spot *is*, so the same offender keeps its number. */
    private static int idFor(LagAggregator.HotSpot spot) {
        String key = spot.kind() + "|" + spot.dimension() + "|" + (spot.chunkX()) + "|" + (spot.chunkZ())
                + "|" + typeIdOf(spot.type());
        Integer existing = SPOT_IDS.get(key);
        if (existing != null) {
            return existing;
        }
        if (SPOT_IDS.size() > 512) {
            SPOT_IDS.clear(); // a long session should not grow this forever
        }
        int id = nextSpotId++;
        SPOT_IDS.put(key, id);
        return id;
    }

    private static String typeIdOf(Object type) {
        if (type instanceof EntityType<?> entityType) {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            return key == null ? "unknown" : key.toString();
        }
        return String.valueOf(type);
    }

    private static Component line(LagAggregator.HotSpot spot, int id) {
        return Component.translatable("tracker.lag.line",
                Component.literal(String.format("#L%04d", id)).withStyle(ChatFormatting.AQUA),
                kindLabel(spot.kind()),
                typeName(spot.type()),
                Integer.toString(spot.count()),
                dimensionName(spot.dimension()),
                spot.x() + ", " + spot.y() + ", " + spot.z(),
                String.format("%.2f", spot.millisPerTick()),
                String.format("%.1f", spot.shareOfAttributed()));
    }

    private static Component kindLabel(String kind) {
        return Component.translatable("tracker.lag.kind." + kind);
    }

    private static Component typeName(Object type) {
        if (type instanceof EntityType<?> entityType) {
            return entityType.getDescription();
        }
        if (type instanceof String id) {
            return Component.literal(id);
        }
        return Component.translatable("tracker.lag.kind.chunk");
    }

    private static String describe(Object type) {
        return typeName(type).getString();
    }

    private static Component dimensionName(Object dimension) {
        if (dimension instanceof Identifier id) {
            return Component.translatableWithFallback(
                    "tracker.dimension." + id.getNamespace() + "." + id.getPath(), id.toString());
        }
        return Component.literal(String.valueOf(dimension));
    }

    /** Interns the dimension for a level (identity-based, allocation free). */
    private static int dimensionId(ServerLevel level) {
        return LagAggregator.dimensionId(level.dimension().identifier());
    }
}
