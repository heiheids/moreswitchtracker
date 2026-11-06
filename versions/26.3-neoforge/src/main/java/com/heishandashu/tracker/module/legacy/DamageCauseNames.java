package com.heishandashu.tracker.module.legacy;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Turns a {@link DamageSource} into a short, readable cause name.
 *
 * <p>Vanilla only offers full sentences ({@code death.attack.lava}), so the tracker keeps its
 * own short labels under {@code tracker.cause.*}. Anything not listed falls back to the raw
 * message id, which still tells the player exactly what happened.</p>
 */
public final class DamageCauseNames {

    private DamageCauseNames() {
    }

    public static Component shortName(DamageSource source) {
        if (source == null) {
            return Component.translatable("tracker.cause.unknown");
        }
        String msgId = source.getMsgId();
        if (msgId == null || msgId.isEmpty()) {
            return Component.translatable("tracker.cause.unknown");
        }
        return switch (msgId) {
            case "lava" -> Component.translatable("tracker.cause.lava");
            case "inFire" -> Component.translatable("tracker.cause.in_fire");
            case "onFire" -> Component.translatable("tracker.cause.on_fire");
            case "hotFloor" -> Component.translatable("tracker.cause.hot_floor");
            case "campfire" -> Component.translatable("tracker.cause.campfire");
            case "cactus" -> Component.translatable("tracker.cause.cactus");
            case "stalagmite" -> Component.translatable("tracker.cause.stalagmite");
            case "sweetBerryBush" -> Component.translatable("tracker.cause.sweet_berry_bush");
            case "fellOutOfWorld" -> Component.translatable("tracker.cause.void");
            case "explosion", "explosion.player" -> Component.translatable("tracker.cause.explosion");
            case "lightningBolt" -> Component.translatable("tracker.cause.lightning");
            case "freeze" -> Component.translatable("tracker.cause.freeze");
            case "outsideBorder" -> Component.translatable("tracker.cause.world_border");
            case "genericKill", "generic" -> Component.translatable("tracker.cause.generic");
            default -> Component.literal(msgId);
        };
    }

    /** Short name for a mob or player that picked something up. */
    public static Component pickerName(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return Component.translatable("tracker.cause.unknown");
        }
        return entity.getDisplayName();
    }
}
