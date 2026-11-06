package com.heishandashu.tracker.client.module.relations;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.RelationNetworking;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the rays: 村民 -> claimed bed (blue) / workstation (red), 僵尸 -> its target (red),
 * 末影龙 -> the player it is after (blue).
 *
 * <p>Endpoints follow the live entity when the client has it, so a chasing zombie's line keeps
 * touching both it and its victim between the once-a-second updates. The coordinates from the
 * packet cover anything not loaded here.</p>
 */
public final class RelationRenderer {

    private static boolean warned;

    /** Time of the previous draw, used to keep a ray alive for exactly one frame. */
    private static long lastFrameNanos;

    /**
     * How long this frame's rays should survive.
     *
     * <p>Minecraft's gizmos are <em>retained</em>: an added line keeps being drawn until its expire
     * time passes, which is why a fast-moving mob appeared to have two or three rays at once. Asking
     * for about half the current frame time keeps the line on screen for the frame it belongs to and
     * drops it on the next one, at any frame rate. (0 would risk the line expiring before it is ever
     * drawn.)</p>
     */
    private static int framePersistenceMillis() {
        long now = System.nanoTime();
        long delta = lastFrameNanos == 0L ? 16_000_000L : now - lastFrameNanos;
        lastFrameNanos = now;
        return (int) Math.max(1L, Math.min(50L, delta / 2_000_000L));
    }

    private RelationRenderer() {
    }

    public static void register() {
        TrackerClientEvents.onGizmos(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null || RelationState.lines().isEmpty()) {
                return;
            }
            try {
                draw(level);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    TrackerMod.LOGGER.warn("[tracker] relation rays could not draw", t);
                }
            }
        });
    }

    /**
     * Draws this frame's rays, each exactly once.
     *
     * <p>Endpoints come from the entities' interpolated positions every frame, so a fast-moving mob
     * drags its line smoothly instead of leaving it stepping once per tick -- tick-stepped updates
     * are what made a quick mob look like it had two or three rays at once. A single render pass
     * issues one draw per relation, and the caller clears the gizmo set each frame, so no
     * duplication can build up.</p>
     */
    private static void draw(ClientLevel level) {
        String dimension = level.dimension().identifier().toString();
        // The dragon is one subject, so only its closest ray is drawn: several sources (a second
        // dragon, or a repeated registration) must not clutter the screen.
        java.util.UUID chosenDragon = nearestDragonSource(level, dimension);
        // Measured once per draw: calling it per line would give the later lines a near-zero delta.
        int persist = framePersistenceMillis();

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
            net.minecraft.gizmos.Gizmos.line(from, to, RelationState.color(line.kind()))
                    .persistForMillis(persist);
        }
    }


    /** The dragon-gaggro source closest to the camera, or null when there is none. */
    private static java.util.UUID nearestDragonSource(ClientLevel level, String dimension) {
        Vec3 eye = Minecraft.getInstance().player == null
                ? Vec3.ZERO : Minecraft.getInstance().player.getEyePosition();
        java.util.UUID best = null;
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
    private static Vec3 endpoint(ClientLevel level, java.util.UUID uuid, double x, double y, double z,
                                 boolean isSource) {
        Entity entity = uuid == null ? null : level.getEntity(uuid);
        if (entity instanceof LivingEntity living) {
            // Start at chest height so the line does not come out of the feet.
            return living.position().add(0.0, isSource ? living.getBbHeight() * 0.6 : living.getBbHeight() * 0.5, 0.0);
        }
        return new Vec3(x, y, z);
    }

    private static java.util.UUID uuid(String raw) {
        try {
            return java.util.UUID.fromString(raw);
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

    /** Not used for drawing, but keeps the module's language keys reachable for future labels. */
    static Component describe(String kind) {
        return Component.translatable("tracker.relation." + kind);
    }
}
