package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.module.legacy.LegacyTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the tracker the real removal reason for tracked dropped items.
 *
 * <p>This is what makes "destroyed by lava" and "the chunk unloaded" distinguishable: both make
 * the item disappear from the level, and only {@link Entity.RemovalReason} tells them apart.</p>
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "remove", at = @At("HEAD"), require = 0)
    private void tracker$onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        // (Object) cast: the mixin class cannot extend Entity, so `this` must be widened first.
        if ((Object) this instanceof ItemEntity item) {
            LegacyTracker.onDropRemoved(item, reason);
        }
    }
}
