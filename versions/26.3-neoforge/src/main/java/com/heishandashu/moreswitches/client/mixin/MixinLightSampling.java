package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.light.HeldLightPacker;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Moving light for the ambient-occlusion light lookup.
 *
 * <p>26.3 uses {@code BlockModelLighter.Cache#getLightCoords} while meshing smooth-lit faces.
 * The flat path goes through {@code BlockModelLighter#getLightCoords}, handled by
 * {@link MixinLightSamplingOuter}; both delegate to {@link HeldLightPacker}.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.BlockModelLighter$Cache")
public class MixinLightSampling {

    @Inject(method = "getLightCoords(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"
            + "Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, require = 1)
    private void moreswitches$addHeldLight(BlockState state, BlockAndTintGetter level,
                                           BlockPos pos,
                                           CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(HeldLightPacker.apply(cir.getReturnValueI(), pos));
    }
}
