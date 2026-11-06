package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.sound.SoundRules;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drops configured sound events before the engine starts playing them. */
@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$silence(SoundInstance instance,
                                      CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (!SoundRules.anyEnabled()) {
            return;
        }
        if (SoundRules.blocked(instance.getIdentifier().toString())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
