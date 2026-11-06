package com.heishandashu.tracker.net;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers every payload channel of the mod in one place.
 *
 * <p>Fabric let each module register its own payload type during {@code onInitialize}. NeoForge
 * instead fires one {@link RegisterPayloadHandlersEvent} and hands out a registrar, so the
 * modules cannot own their registration any more; this class collects them instead. The protocol
 * version string is bumped whenever a payload's shape changes, so a client and server running
 * mismatched versions refuse to talk instead of silently misreading buffers.</p>
 */
@EventBusSubscriber(modid = TrackerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TrackerNetwork {

    /** Bump when a payload layout changes. */
    private static final String PROTOCOL = "1";

    private TrackerNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // ---- 遗产追踪: server -> client snapshot of death points and drop countdowns ----
        registrar.playToClient(DtpNetworking.LegacySyncPayload.TYPE,
                DtpNetworking.LegacySyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.legacy(payload)));

        // ---- 玩家追踪: client -> server watch set, server -> client positions ----
        registrar.playToServer(PlayerTrackNetworking.WatchPayload.TYPE,
                PlayerTrackNetworking.WatchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> com.heishandashu.tracker.module.players.PlayerWatchService
                                .onWatchRequest(context.player(), payload)));
        registrar.playToClient(PlayerTrackNetworking.PositionPayload.TYPE,
                PlayerTrackNetworking.PositionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.positions(payload)));

        // ---- 流浪商人 / 不消失怪物 / 卡顿源头 / 关系射线: server -> client ----
        registrar.playToClient(TraderNetworking.TraderListPayload.TYPE,
                TraderNetworking.TraderListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.traders(payload)));

        registrar.playToClient(PickedMobNetworking.PickedMobListPayload.TYPE,
                PickedMobNetworking.PickedMobListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.pickedMobs(payload)));

        registrar.playToClient(LagNetworking.LagListPayload.TYPE,
                LagNetworking.LagListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.lag(payload)));

        registrar.playToClient(RelationNetworking.RelationListPayload.TYPE,
                RelationNetworking.RelationListPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientPayloadSink.relations(payload)));

        TrackerMod.LOGGER.info("[tracker] 7 payload channel(s) registered (protocol {})", PROTOCOL);
    }

    /** Sending helper: one player. */
    public static void toPlayer(net.minecraft.server.level.ServerPlayer player,
                                net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    /** Sending helper: every connected player. */
    public static void toAll(net.minecraft.server.MinecraftServer server,
                             net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** True when the given buffer type is the one NeoForge hands to handlers. */
    public static boolean usesRegistryBuffer(RegistryFriendlyByteBuf buffer) {
        return buffer != null;
    }
}
