package com.heishandashu.tracker.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The channel used by 不消失怪物追踪 / persistent-mob tracking.
 *
 * <p>The server sends the whole short list of mobs that picked something up, once a second. That
 * single periodic payload is enough to keep positions fresh (a mob in an unloaded chunk cannot
 * move, so its last position stays correct), so no client-to-server channel is needed.</p>
 *
 * <p>Only ids and numbers travel. Names are resolved on the <em>client</em>, in the client's own
 * language, which is what makes the display follow the game language setting.</p>
 */
public final class PickedMobNetworking {

    public static final ResourceLocation LIST_ID = ResourceLocation.fromNamespaceAndPath("tracker", "picked_mobs");

    private PickedMobNetworking() {
    }

    /**
     * One mob that is holding picked-up loot.
     *
     * @param id         display number, 1..99999 (shown as {@code #00001})
     * @param typeId     entity type id, e.g. {@code minecraft:zombie}
     * @param itemId     item id, e.g. {@code minecraft:iron_sword}
     * @param customName custom name if the mob has one, otherwise empty
     */
    public record PickedMobSnap(int id, UUID uuid, String dimension, double x, double y, double z,
                                String typeId, String itemId, int count, String customName) {
    }

    /** The current list. An empty list means "nothing recorded". */
    public record PickedMobListPayload(List<PickedMobSnap> mobs) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PickedMobListPayload> TYPE =
                new CustomPacketPayload.Type<>(LIST_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, PickedMobListPayload> STREAM_CODEC =
                CustomPacketPayload.codec(PickedMobListPayload::write, PickedMobListPayload::new);

        public PickedMobListPayload {
            mobs = List.copyOf(mobs);
        }

        public PickedMobListPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<PickedMobSnap> read(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<PickedMobSnap> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                out.add(new PickedMobSnap(
                        buf.readVarInt(),
                        buf.readUUID(),
                        buf.readUtf(128),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readUtf(128),
                        buf.readUtf(128),
                        buf.readVarInt(),
                        buf.readUtf(64)));
            }
            return out;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.mobs.size());
            for (PickedMobSnap mob : this.mobs) {
                buf.writeVarInt(mob.id());
                buf.writeUUID(mob.uuid());
                buf.writeUtf(mob.dimension());
                buf.writeDouble(mob.x());
                buf.writeDouble(mob.y());
                buf.writeDouble(mob.z());
                buf.writeUtf(mob.typeId());
                buf.writeUtf(mob.itemId());
                buf.writeVarInt(mob.count());
                buf.writeUtf(mob.customName());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
