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
 * The channel shared by every "ray" feature (村民兴趣点 / 僵尸仇恨 / 末影龙仇恨).
 *
 * <p>All three draw the same thing -- a line from a mob to something it is tied to -- so they share
 * one payload instead of three identical pipelines. The {@code kind} tells the client which switch
 * governs the line and what colour to paint it.</p>
 */
public final class RelationNetworking {

    public static final ResourceLocation LIST_ID = ResourceLocation.fromNamespaceAndPath("tracker", "relations");

    /** Kinds, one per feature (and two for the villager feature). */
    public static final String VILLAGER_BED = "villager_bed";
    public static final String VILLAGER_JOB = "villager_job";
    public static final String ZOMBIE_AGGRO = "zombie_aggro";
    public static final String DRAGON_AGGRO = "dragon_aggro";

    private static boolean payloadRegistered;

    private RelationNetworking() {
    }

    /**
     * Kept so the three ray modules can call it without caring about the loader.
     *
     * <p>On NeoForge every channel is registered once in {@code net/TrackerNetwork}, so this is a
     * no-op placeholder rather than a registration.</p>
     */
    public static void registerPayload() {
        // Registration lives in TrackerNetwork.
    }

    /**
     * One ray.
     *
     * @param kind        what the line means, see the constants above
     * @param source      the mob the line starts at
     * @param targetId    the entity the line points at, or {@code ""} when it points at a fixed
     *                    place (a claimed bed or workstation); the client prefers the live entity
     *                    when it has it, and falls back to the coordinates below
     */
    public record RelationSnap(String kind, UUID source, String dimension,
                               double sx, double sy, double sz,
                               String targetId, double tx, double ty, double tz) {
    }

    /** The current set of rays. */
    public record RelationListPayload(List<RelationSnap> relations) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RelationListPayload> TYPE =
                new CustomPacketPayload.Type<>(LIST_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, RelationListPayload> STREAM_CODEC =
                CustomPacketPayload.codec(RelationListPayload::write, RelationListPayload::new);

        public RelationListPayload {
            relations = List.copyOf(relations);
        }

        public RelationListPayload(RegistryFriendlyByteBuf buf) {
            this(read(buf));
        }

        private static List<RelationSnap> read(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<RelationSnap> out = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                out.add(new RelationSnap(buf.readUtf(32), buf.readUUID(), buf.readUtf(128),
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble()));
            }
            return out;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(this.relations.size());
            for (RelationSnap snap : this.relations) {
                buf.writeUtf(snap.kind());
                buf.writeUUID(snap.source());
                buf.writeUtf(snap.dimension());
                buf.writeDouble(snap.sx());
                buf.writeDouble(snap.sy());
                buf.writeDouble(snap.sz());
                buf.writeUtf(snap.targetId());
                buf.writeDouble(snap.tx());
                buf.writeDouble(snap.ty());
                buf.writeDouble(snap.tz());
            }
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
