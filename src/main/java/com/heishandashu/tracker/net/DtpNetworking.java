package com.heishandashu.tracker.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The single server -&gt; client channel used by 遗产追踪.
 *
 * <p>Only the facts the client cannot work out for itself are sent: which deaths are being
 * tracked, where, in which dimension, how long is left, and which dropped items belong to
 * them. Item names come from the client's own copy of the stack, so no item data is sent.</p>
 *
 * <p>26.3 payload shape (copied from vanilla {@code BrandPayload}): the {@code Type} only
 * carries an id, and the codec is built with {@link CustomPacketPayload#codec}.</p>
 */
public final class DtpNetworking {

    public static final ResourceLocation LEGACY_SYNC_ID = ResourceLocation.fromNamespaceAndPath("tracker", "legacy_sync");

    private DtpNetworking() {
    }

    /**
     * One death point.
     *
     * @param remainingTicks ticks left before the drops despawn; frozen while the chunk is
     *                       unloaded, which is exactly the "pause" behaviour we want
     */
    public record DeathSnap(String id, String dimension, int x, int y, int z,
                            int remainingTicks, List<UUID> itemIds) {

        public DeathSnap {
            itemIds = List.copyOf(itemIds);
        }
    }

    /** Every death currently worth drawing. An empty list means "nothing to draw". */
    public record LegacySyncPayload(List<DeathSnap> deaths) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<LegacySyncPayload> TYPE =
                new CustomPacketPayload.Type<>(LEGACY_SYNC_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, LegacySyncPayload> STREAM_CODEC =
                CustomPacketPayload.codec(LegacySyncPayload::write, LegacySyncPayload::new);

        public LegacySyncPayload {
            deaths = List.copyOf(deaths);
        }

        public LegacySyncPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<DeathSnap> read(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<DeathSnap> deaths = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf(32);
                String dimension = buf.readUtf(128);
                int x = buf.readVarInt();
                int y = buf.readVarInt();
                int z = buf.readVarInt();
                int remaining = buf.readVarInt();
                int itemCount = buf.readVarInt();
                List<UUID> items = new ArrayList<>(itemCount);
                for (int j = 0; j < itemCount; j++) {
                    items.add(buf.readUUID());
                }
                deaths.add(new DeathSnap(id, dimension, x, y, z, remaining, items));
            }
            return deaths;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.deaths.size());
            for (DeathSnap death : this.deaths) {
                buf.writeUtf(death.id());
                buf.writeUtf(death.dimension());
                buf.writeVarInt(death.x());
                buf.writeVarInt(death.y());
                buf.writeVarInt(death.z());
                buf.writeVarInt(death.remainingTicks());
                buf.writeVarInt(death.itemIds().size());
                for (UUID uuid : death.itemIds()) {
                    buf.writeUUID(uuid);
                }
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static LegacySyncPayload empty() {
            return new LegacySyncPayload(Collections.emptyList());
        }
    }
}
