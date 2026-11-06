package com.heishandashu.tracker.client.module.relations;

import com.heishandashu.tracker.client.render.WorldPainter;
import com.heishandashu.tracker.compat.ClientEvents;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.RelationNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Draws the rays: 村民 -> claimed bed / workstation, 僵尸 -> its target, 末影龙 -> the player it is
 * after.
 *
 * <p>Endpoints follow the live entity when the client has it, so a chasing zombie's line keeps
 * touching both it and its victim between the once-a-second updates; the coordinates from the
 * packet cover anything not loaded here.</p>
 *
 * <p>The 26.3 build drew these with {@code Gizmos.line}, which turned out to be <em>retained</em>
 * -- a moving line left two or three ghosts behind it. 1.21.1 has no gizmos at all, so the lines
 * are submitted per frame here, which removes that class of artefact entirely.</p>
 */
public final class RelationRenderer {

    private RelationRenderer() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null || RelationState.lines().isEmpty()) {
                return;
            }
            draw(level, pose, buffers);
        });
    }

    private static void draw(ClientLevel level, com.mojang.blaze3d.vertex.PoseStack pose,
                             net.minecraft.client.renderer.MultiBufferSource buffers) {
        String dimension = level.dimension().location().toString();
        // The dragon is one subject, so only its closest ray is drawn: several sources (a second
        // dragon, or a repeated registration) must not clutter the screen.
        UUID chosenDragon = nearestDragonSource(level, dimension);

        WorldPainter.Batch batch = WorldPainter.beginThroughWalls(buffers, pose);
        try {
            for (RelationNetworking.RelationSnap line : RelationState.lines()) {
                if (!line.dimension().equals(dimension) || !enabled(line.kind())) {
                    continue;
                }
                if (RelationNetworking.DRAGON_AGGRO.equals(line.kind())
                        && !line.source().equals(chosenDragon)) {
                    continue; // not the one we are showing
                }
                Vec3 from = endpoint(level, line.source(), line.sx(), line.sy(), line.sz(), true);
                Vec3 to = line.targetId().isEmpty()
                        ? new Vec3(line.tx(), line.ty(), line.tz())
                        : endpoint(level, uuid(line.targetId()), line.tx(), line.ty(), line.tz(), false);
                if (from.distanceToSqr(to) < 0.01) {
                    continue;
                }
                int color = RelationState.color(line.kind());
                batch.line(from, to,
                        ((color >> 16) & 0xFF) / 255.0f,
                        ((color >> 8) & 0xFF) / 255.0f,
                        (color & 0xFF) / 255.0f,
                        ((color >> 24) & 0xFF) / 255.0f);
            }
        } finally {
            batch.close();
        }
    }

    /** The dragon-aggro source closest to the camera, or null when there is none. */
    private static UUID nearestDragonSource(ClientLevel level, String dimension) {
        LocalPlayer player = Minecraft.getInstance().player;
        Vec3 eye = player == null ? Vec3.ZERO : player.getEyePosition();
        UUID best = null;
        double bestDistance = Double.MAX_VALUE;
        for (RelationNetworking.RelationSnap line : RelationState.lines()) {
            if (!RelationNetworking.DRAGON_AGGRO.equals(line.kind()) || !line.dimension().equals(dimension)) {
                continue;
            }
            double distance = eye.distanceToSqr(line.sx(), line.sy(), line.sz());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = line.source();
            }
        }
        return best;
    }

    /** Live position when the client has the entity, otherwise the coordinates from the packet. */
    private static Vec3 endpoint(ClientLevel level, UUID uuid, double x, double y, double z,
                                 boolean isSource) {
        Entity entity = uuid == null ? null
                : com.heishandashu.tracker.client.render.EntityLookup.byUuid(uuid);
        if (entity == null) {
            return new Vec3(x, y, z);
        }
        // Eyes for the source (a line leaving a mob's head reads better than its feet), centre for
        // the target so the line lands in the middle of the bed / player.
        return isSource ? entity.getEyePosition() : entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    private static UUID uuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Only the kinds whose own switch is on are drawn. */
    private static boolean enabled(String kind) {
        return switch (kind) {
            case RelationNetworking.VILLAGER_BED, RelationNetworking.VILLAGER_JOB ->
                    TrackerModules.isEnabled(ModuleIds.VILLAGER_POI_TRACKING);
            case RelationNetworking.ZOMBIE_AGGRO ->
                    TrackerModules.isEnabled(ModuleIds.ZOMBIE_AGGRO_TRACKING);
            case RelationNetworking.DRAGON_AGGRO ->
                    TrackerModules.isEnabled(ModuleIds.DRAGON_AGGRO_TRACKING);
            default -> false;
        };
    }
}
