package com.heishandashu.tracker.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The server-side game-bus events the mod uses, behind one small loader-shaped door.
 *
 * <p>Each helper maps one Fabric event onto its NeoForge counterpart so the feature classes keep
 * reading the way they used to, and so the loader-specific detail lives in exactly one file:</p>
 *
 * <ul>
 *   <li>{@code ServerTickEvents.END_SERVER_TICK} -&gt; {@link ServerTickEvent.Post}</li>
 *   <li>{@code ServerEntityEvents.ENTITY_LOAD} -&gt; {@link EntityJoinLevelEvent}</li>
 *   <li>{@code ServerLivingEntityEvents.AFTER_DEATH} -&gt; {@link LivingDeathEvent}</li>
 * </ul>
 *
 * <p>Server-safe on purpose: this class must never touch a client-only type, because a dedicated
 * server loads it. Client-side helpers live in
 * {@code com.heishandashu.tracker.client.compat.TrackerClientEvents}.</p>
 */
public final class TrackerEvents {

    private TrackerEvents() {
    }

    /** Once per server tick, after the tick -- fires for the integrated server too. */
    public static void onServerTick(Consumer<MinecraftServer> action) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> action.accept(event.getServer()));
    }

    /**
     * Once per server tick, before the tick.
     *
     * <p>Used by the lag profiler, which has to open its stopwatch before the tick starts and read
     * it when the tick ends.</p>
     */
    public static void onServerTickPre(Consumer<MinecraftServer> action) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> action.accept(event.getServer()));
    }

    /**
     * An entity was added to a level (spawn, chunk load, dimension change).
     *
     * <p>Replaces {@code ServerEntityEvents.ENTITY_LOAD}; NeoForge's event fires for entities
     * loaded from disk as well, which is what the wandering-trader scan wants.</p>
     */
    public static void onEntityJoinLevel(BiConsumer<Entity, Level> action) {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) ->
                action.accept(event.getEntity(), event.getLevel()));
    }

    /** A living entity died; the death message has not necessarily been built yet. */
    public static void onLivingDeath(BiConsumer<LivingEntity, DamageSource> action) {
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) ->
                action.accept(event.getEntity(), event.getSource()));
    }
}
