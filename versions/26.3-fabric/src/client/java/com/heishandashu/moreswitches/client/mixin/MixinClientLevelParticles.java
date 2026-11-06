package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.effect.EffectParticleRule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops status-effect particles on the client. */
@Mixin(ClientLevel.class)
public class MixinClientLevelParticles {

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$hideEffectParticles(ParticleOptions options,
                                                  boolean force, boolean alwaysVisible,
                                                  double x, double y, double z,
                                                  double xSpeed, double ySpeed, double zSpeed,
                                                  CallbackInfo cir) {
        if (!EffectParticleRule.blocksEffectParticles()) {
            return;
        }
        if (options.getType() == ParticleTypes.ENTITY_EFFECT
                || options.getType() == ParticleTypes.INSTANT_EFFECT) {
            cir.cancel();
        }
    }
}
