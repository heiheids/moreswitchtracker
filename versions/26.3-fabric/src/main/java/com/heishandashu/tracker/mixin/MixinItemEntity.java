package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.module.legacy.LegacyTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catches everything that can happen to a dropped item: damage, a player walking over it, and
 * two stacks merging.
 *
 * <p>26.3 splits entity damage into {@code hurtServer}/{@code hurtClient}, so the server-side
 * method is the one to hook. The {@link DamageSource} it receives is exactly what the chat
 * message names as the cause.</p>
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    // hurtServer returns boolean, so the handler must take a CallbackInfoReturnable.
    @Inject(method = "hurtServer", at = @At("HEAD"), require = 0)
    private void tracker$onHurtServer(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir) {
        LegacyTracker.onDropDamaged((ItemEntity) (Object) this, source);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), require = 0)
    private void tracker$onPlayerTouch(Player player, CallbackInfo ci) {
        LegacyTracker.onDropTakenByPlayer((ItemEntity) (Object) this, player);
    }

    // Resolved at RETURN: which of the two stacks was actually absorbed is only knowable after
    // vanilla has merged them (the absorbed one is discarded), and guessing it at HEAD gets the
    // direction wrong -- which made the surviving stack look like it had vanished.
    @Inject(method = "tryToMerge", at = @At("RETURN"), require = 0)
    private void tracker$onTryToMerge(ItemEntity other, CallbackInfo ci) {
        LegacyTracker.onMergeResolved((ItemEntity) (Object) this, other);
    }
}
