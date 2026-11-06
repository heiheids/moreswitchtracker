package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.effect.EffectParticleRule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops status-effect particles on the client, so the swirls never become visible particles.
 *
 * <p>The target is the overload of {@code ClientLevel.addParticle} that takes the two boolean
 * flags; that is the one both public entry points funnel into on 1.21.1, so one injection covers
 * the plain path and the "force/decreased" path together.</p>
 *
 * <p>Only the two status-effect types are filtered. {@code ENTITY_EFFECT} is the coloured swirl a
 * living entity emits while a potion is active — the one that flies in front of the camera after
 * drinking something; {@code INSTANT_EFFECT} is the burst an instant potion or a splash produces.
 * Everything else (fire, smoke, splash water, explosions, redstone) is passed straight through.</p>
 *
 * <p>This is purely visual. The effect data lives on the entity and is read by the server and by
 * the client's own effect handling; neither is touched, so duration, amplifier and every gameplay
 * consequence of a potion stay exactly as they were.</p>
 */
@Mixin(ClientLevel.class)
public class MixinClientLevelParticles {

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$hideEffectParticles(ParticleOptions options,
                                                  boolean force,
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
