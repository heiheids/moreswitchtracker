package com.heishandashu.tracker.compat;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server-side event glue: the 1.21.1 NeoForge equivalent of the Fabric callbacks the feature
 * code was written against.
 *
 * <p>The 26.3 build leans on four Fabric APIs ({@code ServerTickEvents},
 * {@code ServerLivingEntityEvents}, {@code ServerEntityEvents}, {@code ServerPlayNetworking}).
 * Rather than rewrite six 100-500 line business classes, this class receives the NeoForge events
 * once and forwards them to subscriber lists the features register into. The feature code then
 * reads the same shape it did before, which is what keeps the port reviewable.</p>
 */
@EventBusSubscriber(modid = TrackerMod.MOD_ID)
public final class ServerEvents {

    /** End-of-server-tick listeners, called once per server tick. */
    private static final List<Consumer<MinecraftServer>> END_TICK = new ArrayList<>();
    /** Start-of-server-tick listeners (卡顿来源追踪 times each tick from here). */
    private static final List<Runnable> START_TICK = new ArrayList<>();
    /** Death listeners: (victim, damage source, level). */
    private static final List<LivingDeath> DEATHS = new ArrayList<>();
    /** Entity-join listeners: (entity, level). */
    private static final List<EntityJoin> JOINS = new ArrayList<>();
    /** Server-stopping listeners, so per-world state can be dropped. */
    private static final List<Runnable> STOPPING = new ArrayList<>();

    private ServerEvents() {
    }

    /** Marker interfaces so the lambdas below stay readable at a glance. */
    @FunctionalInterface
    public interface LivingDeath {
        void onDeath(LivingEntity victim, DamageSource source, ServerLevel level);
    }

    @FunctionalInterface
    public interface EntityJoin {
        void onJoin(Entity entity, ServerLevel level);
    }

    // ---- registration, called from the feature modules -------------------------------------

    public static void onEndTick(Consumer<MinecraftServer> listener) {
        END_TICK.add(listener);
    }

    public static void onStartTick(Runnable listener) {
        START_TICK.add(listener);
    }

    public static void onLivingDeath(LivingDeath listener) {
        DEATHS.add(listener);
    }

    public static void onEntityJoin(EntityJoin listener) {
        JOINS.add(listener);
    }

    public static void onServerStopping(Runnable listener) {
        STOPPING.add(listener);
    }

    // ---- NeoForge events -------------------------------------------------------------------

    @SubscribeEvent
    public static void startTick(ServerTickEvent.Pre event) {
        for (Runnable listener : START_TICK) {
            listener.run();
        }
    }

    @SubscribeEvent
    public static void endTick(ServerTickEvent.Post event) {
        for (Consumer<MinecraftServer> listener : END_TICK) {
            listener.accept(event.getServer());
        }
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        for (LivingDeath listener : DEATHS) {
            listener.onDeath(event.getEntity(), event.getSource(), level);
        }
    }

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (EntityJoin listener : JOINS) {
            listener.onJoin(event.getEntity(), level);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (Runnable listener : STOPPING) {
            listener.run();
        }
    }

    /** Convenience for the features that send to one player. */
    public static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    /** Convenience for the features that broadcast. */
    public static void sendAll(MinecraftServer server, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
