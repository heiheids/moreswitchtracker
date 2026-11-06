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
 * The two channels used by 玩家追踪.
 *
 * <p>The client cannot know where a player is once that player leaves its entity-sync range: the
 * entity is simply gone from the client level. To keep a highlight visible anywhere in the same
 * dimension, the client tells the server who it is watching ({@link WatchPayload}) and the server
 * answers with those positions ({@link PositionPayload}).</p>
 *
 * <p>Position is sent as a dimension id plus doubles, so it stays exact at long range.</p>
 */
public final class PlayerTrackNetworking {

    public static final ResourceLocation WATCH_ID = ResourceLocation.fromNamespaceAndPath("tracker", "player_watch");
    public static final ResourceLocation POSITION_ID = ResourceLocation.fromNamespaceAndPath("tracker", "player_position");

    private PlayerTrackNetworking() {
    }

    /** One watched player's position. */
    public record Position(UUID uuid, String dimension, double x, double y, double z) {
    }

    /** client -&gt; server: the full set of players this client wants positions for. */
    public record WatchPayload(List<UUID> targets) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<WatchPayload> TYPE =
                new CustomPacketPayload.Type<>(WATCH_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, WatchPayload> STREAM_CODEC =
                CustomPacketPayload.codec(WatchPayload::write, WatchPayload::new);

        public WatchPayload {
            targets = List.copyOf(targets);
        }

        public WatchPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<UUID> read(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<UUID> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(buf.readUUID());
            }
            return ids;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.targets.size());
            for (UUID id : this.targets) {
                buf.writeUUID(id);
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** server -&gt; client: where the watched players are right now. */
    public record PositionPayload(List<Position> positions) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<PositionPayload> TYPE =
                new CustomPacketPayload.Type<>(POSITION_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, PositionPayload> STREAM_CODEC =
                CustomPacketPayload.codec(PositionPayload::write, PositionPayload::new);

        public PositionPayload {
            positions = List.copyOf(positions);
        }

        public PositionPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<Position> read(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<Position> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                out.add(new Position(buf.readUUID(), buf.readUtf(128),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            return out;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.positions.size());
            for (Position position : this.positions) {
                buf.writeUUID(position.uuid());
                buf.writeUtf(position.dimension());
                buf.writeDouble(position.x());
                buf.writeDouble(position.y());
                buf.writeDouble(position.z());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
