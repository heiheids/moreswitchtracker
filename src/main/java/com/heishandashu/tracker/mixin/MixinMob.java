package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.module.legacy.LegacyTracker;
import com.heishandashu.tracker.module.persistent.PersistentMobTrackingModule;
import com.heishandashu.tracker.module.persistent.PickedMobTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports mobs picking dropped items up.
 *
 * <p>{@code Mob#pickUpItem} is the shared entry point used by {@code Mob#aiStep}, which covers
 * zombies, skeletons, piglins and friends. Allays and villagers take a different path
 * ({@code InventoryCarrier#pickUpItem}), which is not hooked yet.</p>
 *
 * <p>1.21.1 difference: the signature is {@code pickUpItem(ItemEntity)} with no {@code ServerLevel}
 * argument (26.x added one), so the level is taken from the mob itself.</p>
 */
@Mixin(Mob.class)
public abstract class MixinMob {


    // require = 1: this hook *is* the feature (two of them, in fact). A silent no-op here would
    // look exactly like "no mob ever picked anything up".
    @Inject(method = "pickUpItem", at = @At("HEAD"), require = 1)
    private void tracker$onPickUpItem(ItemEntity item, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }


        // Two independent features watch the same vanilla event; neither reads the other's state.
        LegacyTracker.onDropTakenByMob(item, mob);
        PickedMobTracker.onPickup(mob, level, item);
    }
}
