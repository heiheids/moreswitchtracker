package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.sound.SoundRules;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the configured sound events before the engine starts playing them.
 *
 * <p>This is the one place every sound passes through: sounds sent by the server as position packets,
 * sounds bound to an entity, and long-lived looping instances such as the minecart rattle all end up
 * here as a {@code SoundInstance}, so a single check covers every path.</p>
 *
 * <p>Cancelling the call is equivalent to the sound never starting. Nothing else changes: the sound
 * is not muted globally, and turning the switch off brings it straight back.</p>
 *
 * <p>On 1.21.1 the engine's play method returns nothing, so this cancels rather than substituting a
 * result, and the sound identifier is read from {@code getLocation()}.</p>
 */
@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$silence(SoundInstance instance, CallbackInfo cir) {
        if (!SoundRules.anyEnabled()) {
            return;
        }
        if (SoundRules.blocked(instance.getLocation().toString())) {
            cir.cancel();
        }
    }
}
