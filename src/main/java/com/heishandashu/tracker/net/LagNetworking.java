package com.heishandashu.tracker.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The channel used by 卡顿来源追踪 / lag source tracking.
 *
 * <p>The server measures tick cost and pushes the current hot spots. Sent every window (30
 * seconds) whether or not anything is bad enough to announce, so the manual {@code /tracker lag}
 * query always has fresh data to show.</p>
 */
public final class LagNetworking {

    public static final ResourceLocation LIST_ID = ResourceLocation.fromNamespaceAndPath("tracker", "lag_sources");

    private LagNetworking() {
    }

    /**
     * One measured hot spot.
     *
     * @param id             display number, shown as {@code #L0001} (the L keeps it distinct from
     *                       the {@code #00001} ids of 不消失怪物追踪)
     * @param typeId         entity/block-entity id, or {@code "chunk"} for chunk-level cost
     */
    public record LagSnap(int id, String kind, String typeId, int count, String dimension,
                          double x, double y, double z, double millisPerTick, double share) {
    }

    /** The current list. An empty list means nothing was measured this window. */
    public record LagListPayload(List<LagSnap> spots) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<LagListPayload> TYPE =
                new CustomPacketPayload.Type<>(LIST_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, LagListPayload> STREAM_CODEC =
                CustomPacketPayload.codec(LagListPayload::write, LagListPayload::new);

        public LagListPayload {
            spots = List.copyOf(spots);
        }

        public LagListPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<LagSnap> read(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<LagSnap> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                out.add(new LagSnap(buf.readVarInt(), buf.readUtf(32), buf.readUtf(128), buf.readVarInt(),
                        buf.readUtf(128), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble()));
            }
            return out;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.spots.size());
            for (LagSnap spot : this.spots) {
                buf.writeVarInt(spot.id());
                buf.writeUtf(spot.kind());
                buf.writeUtf(spot.typeId());
                buf.writeVarInt(spot.count());
                buf.writeUtf(spot.dimension());
                buf.writeDouble(spot.x());
                buf.writeDouble(spot.y());
                buf.writeDouble(spot.z());
                buf.writeDouble(spot.millisPerTick());
                buf.writeDouble(spot.share());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
