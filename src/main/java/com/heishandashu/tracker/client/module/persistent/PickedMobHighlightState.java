package com.heishandashu.tracker.client.module.persistent;

import com.heishandashu.tracker.net.PickedMobNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the client knows about 不消失怪物追踪: the list the server sent, plus which mobs the player
 * asked to highlight and for how much longer.
 *
 * <p>Names are resolved here, from the ids the server sent, so they come out in whatever language
 * the game is set to rather than the language of whoever runs the server.</p>
 */
public final class PickedMobHighlightState {

    /** Ten minutes, in client ticks. */
    public static final int HIGHLIGHT_TICKS = 12000;

    private static final Map<Integer, PickedMobNetworking.PickedMobSnap> RECORDS = new LinkedHashMap<>();
    private static final Map<UUID, Highlight> HIGHLIGHTS = new LinkedHashMap<>();

    private static int clientTicks;

    private PickedMobHighlightState() {
    }

    /** A mob the player asked to track, with the id they typed. */
    public record Highlight(int id, UUID uuid, String name, int untilTick) {

        public int remaining() {
            return Math.max(0, this.untilTick - clientTicks);
        }
    }

    /** Called when the server pushes the list. */
    public static void accept(PickedMobNetworking.PickedMobListPayload payload) {
        RECORDS.clear();
        for (PickedMobNetworking.PickedMobSnap snap : payload.mobs()) {
            RECORDS.put(snap.id(), snap);
        }
    }

    public static void tick() {
        clientTicks++;
        HIGHLIGHTS.entrySet().removeIf(entry -> clientTicks >= entry.getValue().untilTick());
    }

    /** Highlights a recorded mob by its display number. @return false when the id is unknown */
    public static boolean highlight(int id) {
        PickedMobNetworking.PickedMobSnap snap = RECORDS.get(id);
        if (snap == null) {
            return false;
        }
        HIGHLIGHTS.put(snap.uuid(), new Highlight(id, snap.uuid(), displayName(snap),
                clientTicks + HIGHLIGHT_TICKS));
        return true;
    }

    public static List<Highlight> highlights() {
        return new ArrayList<>(HIGHLIGHTS.values());
    }

    public static boolean hasRecords() {
        return !RECORDS.isEmpty();
    }

    public static void clear() {
        RECORDS.clear();
        HIGHLIGHTS.clear();
    }

    /** Where the mob is now; the entity wins when it is loaded on this side. */
    public static Vec3 position(PickedMobNetworking.PickedMobSnap snap) {
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            var entity = com.heishandashu.tracker.client.render.EntityLookup.byUuid(snap.uuid());
            if (entity != null) {
                return entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
            }
        }
        return new Vec3(snap.x(), snap.y() + 0.9, snap.z());
    }

    public static PickedMobNetworking.PickedMobSnap record(UUID uuid) {
        for (PickedMobNetworking.PickedMobSnap snap : RECORDS.values()) {
            if (snap.uuid().equals(uuid)) {
                return snap;
            }
        }
        return null;
    }

    /** Mob name in the client's language: custom name, else the entity type's translated name. */
    public static String displayName(PickedMobNetworking.PickedMobSnap snap) {
        if (snap.customName() != null && !snap.customName().isEmpty()) {
            return snap.customName();
        }
        ResourceLocation typeId = ResourceLocation.tryParse(snap.typeId());
        EntityType<?> type = typeId == null ? null : BuiltInRegistries.ENTITY_TYPE.get(typeId);
        if (type == null) {
            return snap.typeId();
        }
        return type.getDescription().getString();
    }

    /** Item name in the client's language. */
    public static Component itemName(PickedMobNetworking.PickedMobSnap snap) {
        ResourceLocation itemId = ResourceLocation.tryParse(snap.itemId());
        var item = itemId == null ? null : BuiltInRegistries.ITEM.get(itemId);
        return item == null ? Component.literal(snap.itemId()) : item.getDefaultInstance().getHoverName();
    }

    public static String dimension(PickedMobNetworking.PickedMobSnap snap) {
        ResourceLocation id = ResourceLocation.tryParse(snap.dimension());
        if (id == null) {
            return snap.dimension();
        }
        return Component.translatableWithFallback(
                "tracker.dimension." + id.getNamespace() + "." + id.getPath(), snap.dimension()).getString();
    }
}
