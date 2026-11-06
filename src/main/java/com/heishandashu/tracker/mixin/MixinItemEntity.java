package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.module.legacy.LegacyTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catches everything that can happen to a dropped item: damage and a player walking over it.
 *
 * <p>1.21.1 differences from the 26.3 build, both real:</p>
 * <ul>
 *   <li>damage is a single {@code hurt(DamageSource, float)} -- 26.x split it into
 *       {@code hurtServer}/{@code hurtClient};</li>
 *   <li>there is no {@code tryToMerge}, so stack merging is not hooked here. That hook only ever
 *       refined the drop list's appearance (it resolved which stack survived a merge), so its
 *       absence degrades the display slightly instead of losing a tracking event.</li>
 * </ul>
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    // hurt() returns boolean, so the handler must take a CallbackInfoReturnable.
    @Inject(method = "hurt", at = @At("HEAD"), require = 0)
    private void tracker$onHurt(DamageSource source, float amount,
                                CallbackInfoReturnable<Boolean> cir) {
        LegacyTracker.onDropDamaged((ItemEntity) (Object) this, source);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), require = 0)
    private void tracker$onPlayerTouch(Player player, CallbackInfo ci) {
        LegacyTracker.onDropTakenByPlayer((ItemEntity) (Object) this, player);
    }
}
