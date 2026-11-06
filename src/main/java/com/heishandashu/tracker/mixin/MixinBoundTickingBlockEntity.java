package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.module.lag.LagProfiler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times one ticking block entity, for 卡顿来源追踪.
 *
 * <p>This is where {@code Level#tickBlockEntities} actually runs a furnace/hopper/etc., and the
 * wrapper already holds the block entity, so the level (and therefore the dimension) comes from
 * there rather than from any guesswork.</p>
 *
 * <p>Note on the {@code @Shadow} field: the target declares it as {@code private final T
 * blockEntity} with {@code T extends BlockEntity}. Generic erasure makes the descriptor
 * {@code Lnet/minecraft/world/level/block/entity/BlockEntity;}, so shadowing it as a plain
 * {@link BlockEntity} -- rather than declaring a type variable here -- is what matches at runtime.</p>
 */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class MixinBoundTickingBlockEntity {

    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), require = 1)
    private void tracker$lagBlockEntityStart(CallbackInfo ci) {
        LagProfiler.blockEntityStart();
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 1)
    private void tracker$lagBlockEntityEnd(CallbackInfo ci) {
        LagProfiler.blockEntityEnd((TickingBlockEntity) (Object) this, this.blockEntity.getLevel());
    }
}
