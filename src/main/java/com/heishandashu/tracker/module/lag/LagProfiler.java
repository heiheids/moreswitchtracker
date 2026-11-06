package com.heishandashu.tracker.module.lag;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.net.LagNetworking;
import com.heishandashu.tracker.compat.ServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
     * Sampled ticks per window: 120 samples x 5 ticks = 600 ticks. That is 30 seconds at 20 tps and
     * 10 seconds at 60 tps, so the report's "last N seconds" comes from the measured wall-clock
     * length of the window rather than from an assumed tick rate -- a 60 tps server made the old
     * fixed "30 seconds" text wrong.
     */
    private static final int SAMPLES_PER_WINDOW = 120;
    /** How many entries a report shows. */
    private static final int REPORT_LIMIT = 5;
    /** How many by-type totals a report shows. */
    private static final int TYPE_REPORT_LIMIT = 3;
    /** A single measurement longer than this is treated as a stall and ignored. */
    private static final long MAX_MEASUREMENT_NANOS = 500_000_000L;

    /** Whether a report has already failed, so the error is logged once instead of every window. */
    private static boolean reportFailed;

    /** Hot-spot key to the number the player sees, so the same offender keeps its {@code #L} id. */
    private static final Map<String, Integer> SPOT_IDS = new HashMap<>();
    private static int nextSpotId = 1;

    // Separate start slots per kind: an entity tick can happen while a chunk is being ticked, and
    // sharing one slot would corrupt both.
    private static long entityStart;
    private static long blockEntityStart;
    private static long chunkStart;

    private static int tickCounter;
    /** Wall-clock start of the current window, for the report's "last N seconds" figure. */
    private static long windowStartMillis;
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
        ServerEvents.onStartTick(LagProfiler::beginTick);
        ServerEvents.onEndTick(LagProfiler::endTick);
    }

    /** Number of ticks actually measured in the current window; the divisor for ms/tick. */
    public static int sampledTicks() {
        return sampledTicks;
    }

    /**
     * Real ticks elapsed in the current window, i.e. {@code SAMPLES_PER_WINDOW * SAMPLE_EVERY}.
     *
     * <p>This -- not {@link #sampledTicks()} -- is the divisor that turns accumulated nanoseconds
     * into a per-tick cost comparable with the server's mspt, because only one tick in
     * {@code SAMPLE_EVERY} is actually measured.</p>
     */
    public static int windowTicks() {
        return SAMPLES_PER_WINDOW * SAMPLE_EVERY;
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
            if (sampledTicks == 0) {
                // First sample of a new window: the report's "last N seconds" is measured from here,
                // so it stays right at any tick rate (600 ticks is 30 s at 20 tps, 10 s at 60 tps).
                windowStartMillis = System.currentTimeMillis();
            }
            sampledTicks++;
        }
        if (sampledTicks < SAMPLES_PER_WINDOW) {
            return;
        }
        // A diagnostic feature must never be able to kill the server tick loop: a bug in the
        // report (it once threw an NPE out of here) crashed the whole game. Report failures are
        // logged once and the window simply restarts.
        try {
            if (LagSourceTrackingModule.isActive()) {
                report(server);
            }
        } catch (Throwable t) {
            if (!reportFailed) {
                reportFailed = true;
                TrackerMod.LOGGER.error("[tracker] lag report failed -- measurement continues", t);
            }
        } finally {
            LagAggregator.reset();
            sampledTicks = 0;
        }
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
                entityTypeId(entity.getType()), elapsed, pos.getX(), pos.getY(), pos.getZ());
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
        LagAggregator.record("block_entity", LagAggregator.dimensionId(level.dimension().location()),
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
            snaps.add(new LagNetworking.LagSnap(id, spot.kind(), spot.typeId(), spot.count(),
                    String.valueOf(spot.dimension()),
                    spot.x(), spot.y(), spot.z(), spot.millisPerTick(), spot.shareOfAttributed()));
        }

        // Always push the list, even when nothing is bad enough to announce: the manual query and
        // the #L highlight both need current data.
        LagNetworking.LagListPayload payload = new LagNetworking.LagListPayload(snaps);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerEvents.send(player, payload);
        }

        // Report every window, unconditionally.
        //
        // This used to be gated ("only when a chunk costs 5 ms/tick", later "or the server is
        // slow"), and every gate turned out to be wrong for a real world: cost spread over
        // hundreds of chunks never crosses a per-chunk threshold, a 5 ms/chunk bar is unreachable
        // on a healthy server, and an absolute MSPT bar is meaningless once the tick rate is not
        // 20. The switch is off by default and is turned on deliberately, so "on = you get the
        // numbers" is the behaviour a player can predict.
        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        long elapsedSeconds = windowStartMillis == 0L
                ? 0L : Math.max(1L, (System.currentTimeMillis() - windowStartMillis) / 1000L);

        Component header = Component.translatable("tracker.lag.header",
                Long.toString(elapsedSeconds),
                String.format("%.1f", mspt));
        Component hint = Component.translatable("tracker.lag.hint_manual").withStyle(ChatFormatting.GRAY);

        // Log the stable id, not the localized name. Component#getString() returns text in the
        // server's language, which lands in the log as mojibake on a non-UTF-8 console and is
        // useless for grepping anyway; the localized name is still shown in chat below.
        TrackerMod.LOGGER.info("[tracker] lag: worst {} ({}) at {} {} -- {} ms/tick over {} s, {} groups",
                top.get(0).typeId(), top.get(0).kind(),
                top.get(0).dimension(), top.get(0).x() + "," + top.get(0).y() + "," + top.get(0).z(),
                String.format("%.2f", top.get(0).millisPerTick()),
                elapsedSeconds, LagAggregator.groupCount());

        List<LagAggregator.TypeTotal> typeTotals = LagAggregator.topTypes(TYPE_REPORT_LIMIT);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(header);
            for (int i = 0; i < top.size(); i++) {
                player.sendSystemMessage(line(top.get(i), snaps.get(i).id()));
            }
            if (!typeTotals.isEmpty()) {
                player.sendSystemMessage(Component.translatable("tracker.lag.types_header")
                        .withStyle(ChatFormatting.GRAY));
                for (LagAggregator.TypeTotal total : typeTotals) {
                    player.sendSystemMessage(Component.translatable("tracker.lag.type_total",
                            typeLabel(total.kind(), total.typeId()),
                            Integer.toString(total.count()),
                            String.format("%.2f", total.millisPerTick()),
                            String.format("%.1f", total.shareOfAttributed())));
                }
            }
            player.sendSystemMessage(hint);
        }
    }

    /** Ids keyed by what the spot *is*, so the same offender keeps its number. */
    private static int idFor(LagAggregator.HotSpot spot) {
        String key = spot.kind() + "|" + spot.dimension() + "|" + (spot.chunkX()) + "|" + (spot.chunkZ())
                + "|" + spot.typeId();
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

    /** Registry id for an entity type, used as the aggregation key. */
    private static String entityTypeId(EntityType<?> type) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return key == null ? "unknown" : key.toString();
    }

    private static Component line(LagAggregator.HotSpot spot, int id) {
        return Component.translatable("tracker.lag.line",
                Component.literal(String.format("#L%04d", id)).withStyle(ChatFormatting.AQUA),
                kindLabel(spot.kind()),
                typeName(spot),
                Integer.toString(spot.count()),
                dimensionName(spot.dimension()),
                spot.x() + ", " + spot.y() + ", " + spot.z(),
                String.format("%.2f", spot.millisPerTick()),
                String.format("%.1f", spot.shareOfAttributed()));
    }

    private static Component kindLabel(String kind) {
        return Component.translatable("tracker.lag.kind." + kind);
    }

    /**
     * The "what" column of a report row.
     *
     * <p>A chunk has no "what" of its own -- the kind column already says it is a chunk -- so that
     * column is left empty rather than repeating the label. Block entity types are named after
     * their block, so the vanilla {@code block.<namespace>.<path>} key resolves them to a readable
     * name; the id is the fallback for the few types whose id and block differ (e.g.
     * {@code minecraft:skull}).</p>
     */
    private static Component typeName(LagAggregator.HotSpot spot) {
        if ("chunk".equals(spot.kind())) {
            return Component.empty();
        }
        return typeLabel(spot.kind(), spot.typeId());
    }

    /**
     * A readable name for one source type id.
     *
     * <p>Entity ids resolve through the entity registry, which gives the localised name the player
     * knows. Block entity types are named after their block, so the vanilla
     * {@code block.<namespace>.<path>} key resolves them; the raw id is the fallback for the few
     * types whose id and block differ (e.g. {@code minecraft:skull}).</p>
     */
    private static Component typeLabel(String kind, String typeId) {
        if (typeId == null) {
            return Component.literal("unknown");
        }
        if ("chunk".equals(kind)) {
            return Component.translatable("tracker.lag.kind.chunk");
        }
        ResourceLocation id = ResourceLocation.tryParse(typeId);
        if (id == null) {
            return Component.literal(typeId);
        }
        if ("entity".equals(kind)) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type != null) {
                return type.getDescription();
            }
        } else if ("block_entity".equals(kind)) {
            return Component.translatableWithFallback(
                    "block." + id.getNamespace() + "." + id.getPath(), typeId);
        }
        return Component.literal(typeId);
    }

    private static Component dimensionName(Object dimension) {
        if (dimension instanceof ResourceLocation id) {
            return Component.translatableWithFallback(
                    "tracker.dimension." + id.getNamespace() + "." + id.getPath(), id.toString());
        }
        return Component.literal(String.valueOf(dimension));
    }

    /** Interns the dimension for a level (identity-based, allocation free). */
    private static int dimensionId(ServerLevel level) {
        return LagAggregator.dimensionId(level.dimension().location());
    }
}
