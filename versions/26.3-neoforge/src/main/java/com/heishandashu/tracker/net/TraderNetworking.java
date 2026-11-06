package com.heishandashu.tracker.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The channel used by 流浪商人追踪 / wandering trader tracking.
 *
 * <p>The trader's despawn countdown is a server-side field that is never sent to clients, so the
 * client cannot show it without help. The server pushes the list of known traders once a second,
 * together with their current countdown.</p>
 */
public final class TraderNetworking {

    public static final Identifier LIST_ID = Identifier.fromNamespaceAndPath("tracker", "traders");

    private TraderNetworking() {
    }

    /**
     * One known wandering trader.
     *
     * @param despawnTicks ticks left before vanilla removes the trader. This is {@code 0} for a
     *                     trader that will never despawn: the constructor initialises it to zero
     *                     and only vanilla's own spawner calls {@code setDespawnDelay(48000)}, so a
     *                     spawn-egg or {@code /summon} trader keeps zero and stays forever
     * @param justDetected true on the first sync after this trader was noticed, so the client can
     *                     play the arrival alert without guessing from the countdown
     */
    public record TraderSnap(UUID uuid, String dimension, double x, double y, double z,
                             int despawnTicks, boolean justDetected) {
    }

    /** The current list. An empty list means no wandering trader is being tracked. */
    public record TraderListPayload(List<TraderSnap> traders) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<TraderListPayload> TYPE =
                new CustomPacketPayload.Type<>(LIST_ID);

        public static final StreamCodec<FriendlyByteBuf, TraderListPayload> STREAM_CODEC =
                CustomPacketPayload.codec(TraderListPayload::write, TraderListPayload::new);

        public TraderListPayload {
            traders = List.copyOf(traders);
        }

        public TraderListPayload(FriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<TraderSnap> read(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<TraderSnap> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                out.add(new TraderSnap(buf.readUUID(), buf.readUtf(128),
                        buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(),
                        buf.readBoolean()));
            }
            return out;
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.traders.size());
            for (TraderSnap trader : this.traders) {
                buf.writeUUID(trader.uuid());
                buf.writeUtf(trader.dimension());
                buf.writeDouble(trader.x());
                buf.writeDouble(trader.y());
                buf.writeDouble(trader.z());
                buf.writeVarInt(trader.despawnTicks());
                buf.writeBoolean(trader.justDetected());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
