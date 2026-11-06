package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops campfires from producing smoke.
 *
 * <p>{@code makeParticles} is the one place a campfire creates its plume, and it is shared by lit
 * campfires, soul campfires and the signal variant on a hay bale, so cancelling it removes every
 * kind of campfire smoke in one step rather than filtering particles by type afterwards.</p>
 *
 * <p>The campfire itself is untouched: it still burns, still cooks, still lights the area and
 * still shows its flames. Only the smoke, which is the part that obscures the view, is gone.</p>
 */
@Mixin(CampfireBlock.class)
public class MixinCampfireBlock {

    @Inject(method = "makeParticles", at = @At("HEAD"), cancellable = true, require = 1)
    private static void moreswitches$suppressSmoke(Level level, BlockPos pos,
                                                   boolean isSignalFire, boolean isCosy,
                                                   CallbackInfo cir) {
        if (QuietRules.suppressCampfireSmoke()) {
            cir.cancel();
        }
    }
}
