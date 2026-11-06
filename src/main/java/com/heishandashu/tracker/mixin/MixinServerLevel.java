package com.heishandashu.tracker.mixin;

import com.heishandashu.tracker.module.lag.LagProfiler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Times each entity and each chunk the server ticks, for 卡顿来源追踪.
 *
 * <p>Both targets are public vanilla entry points that already handle one item at a time, so the
 * measurement needs no clever injection -- just a start and an end around the call. Signatures are
 * unchanged between 1.21.1 and 26.x.</p>
 */
@Mixin(ServerLevel.class)
public abstract class MixinServerLevel {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), require = 1)
    private void tracker$lagEntityStart(Entity entity, CallbackInfo ci) {
        LagProfiler.entityStart();
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"), require = 1)
    private void tracker$lagEntityEnd(Entity entity, CallbackInfo ci) {
        LagProfiler.entityEnd((ServerLevel) (Object) this, entity);
    }

    @Inject(method = "tickChunk", at = @At("HEAD"), require = 1)
    private void tracker$lagChunkStart(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        LagProfiler.chunkStart();
    }

    @Inject(method = "tickChunk", at = @At("RETURN"), require = 1)
    private void tracker$lagChunkEnd(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        LagProfiler.chunkEnd((ServerLevel) (Object) this, chunk);
    }
}
