package com.heishandashu.tracker.net;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's single place where payload channels are declared, NeoForge style.
 *
 * <p>Fabric let each module register its payload type directly against
 * {@code PayloadTypeRegistry}. NeoForge instead hands out one registrar per mod from
 * {@link RegisterPayloadHandlersEvent}, and that event fires after the mod constructor has run.
 * So modules queue their channels here during {@code registerCommon()} and the event drains the
 * queue a moment later. Each module still owns its own channel; only the moment of registration
 * moved.</p>
 *
 * <p>Channels are declared for both sides even while a feature is switched off -- exactly like
 * the Fabric version -- because a payload type must exist before anything is sent on it.</p>
 */
public final class TrackerNetwork {

    /** Declares one channel against NeoForge's registrar. */
    private interface Channel {
        void declare(PayloadRegistrar registrar);
    }

    private static final List<Channel> PENDING = new ArrayList<>();
    private static boolean drained;

    private TrackerNetwork() {
    }

    /** server -&gt; client channel; the client half registers its handler via the client bus. */
    public static <T extends CustomPacketPayload> void clientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PENDING.add(registrar -> registrar.playToClient(type, codec));
    }

    /** client -&gt; server channel handled on the server. */
    public static <T extends CustomPacketPayload> void serverbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            IPayloadHandler<T> handler) {
        PENDING.add(registrar -> registrar.playToServer(type, codec, handler));
    }

    /**
     * Drains everything the modules queued. Called from the mod bus during payload registration.
     *
     * <p>Modelled on {@code BrandPayload} / vanilla's own payload shape: the type carries only an
     * id and the codec is attached here.</p>
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        if (drained) {
            // Registration happens once per game run; a second call would be a mod-list mistake,
            // not something to silently redo.
            TrackerMod.LOGGER.warn("[tracker] payload registration ran twice -- ignoring the extra call");
            return;
        }
        drained = true;

        PayloadRegistrar registrar = event.registrar("1");
        for (Channel channel : PENDING) {
            channel.declare(registrar);
        }
        TrackerMod.LOGGER.info("[tracker] {} payload channel(s) registered", PENDING.size());
        PENDING.clear();
    }

    // ------------------------------------------------------------------
    // Sending
    // ------------------------------------------------------------------

    /** server -&gt; one client. */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
