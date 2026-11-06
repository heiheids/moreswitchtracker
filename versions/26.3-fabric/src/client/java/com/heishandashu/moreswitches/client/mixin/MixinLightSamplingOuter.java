package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.light.HeldLightPacker;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Moving light for the flat-lit light lookup.
 *
 * <p>26.3's flat path calls {@code BlockModelLighter#getLightCoords} directly; the
 * ambient-occlusion path calls the thread-local cache, handled by {@link MixinLightSampling}.
 * Both delegate to {@link HeldLightPacker} so the switch works in both graphics modes.</p>
 */
@Mixin(BlockModelLighter.class)
public class MixinLightSamplingOuter {

    @Inject(method = "getLightCoords(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void moreswitches$addHeldLightFlat(BlockState state, BlockAndTintGetter level,
                                               BlockPos pos,
                                               CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(HeldLightPacker.apply(cir.getReturnValueI(), pos));
    }
}
