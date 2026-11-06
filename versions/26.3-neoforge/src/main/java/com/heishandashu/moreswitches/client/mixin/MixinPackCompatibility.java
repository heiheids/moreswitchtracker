package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets an incompatible resource pack be selected without the version-mismatch warning.
 *
 * <p>Vanilla asks {@code PackCompatibility.isCompatible()} before enabling a pack; a false answer
 * makes the pack list raise a confirmation dialog saying the pack was built for another version.
 * Answering true makes that path take the "just enable it" branch instead.</p>
 *
 * <p>Only the warning is skipped. The pack is still loaded the same way, and a pack that is
 * genuinely missing is removed by a separate existence check that this does not touch.</p>
 */
@Mixin(PackCompatibility.class)
public class MixinPackCompatibility {

    @Inject(method = "isCompatible", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$skipWarning(CallbackInfoReturnable<Boolean> cir) {
        if (QuietRules.skipResourcePackWarning()) {
            cir.setReturnValue(true);
        }
    }
}
