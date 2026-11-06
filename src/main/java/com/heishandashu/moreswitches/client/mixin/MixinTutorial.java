package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.client.tutorial.Tutorial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the tutorial from starting, so none of its messages ever appear.
 *
 * <p>{@code Tutorial.start()} is the single place the tutorial picks its step for a freshly joined
 * world. Cancelling it leaves the internal step instance unset, and every callback the tutorial
 * exposes — {@code tick}, {@code onMouse}, {@code onInput}, {@code onGetItem}, {@code stop} —
 * already checks for that and does nothing.</p>
 *
 * <p>Stopping it at the source is what makes this cover "any tutorial message": the walk, jump,
 * punch-tree, inventory and crafting prompts are all steps of this one machine, so none of them
 * can be reached. Trying to hide the toasts one by one would be a chase.</p>
 */
@Mixin(Tutorial.class)
public class MixinTutorial {

    @Inject(method = "start", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$suppressTutorial(CallbackInfo cir) {
        if (QuietRules.suppressTutorial()) {
            cir.cancel();
        }
    }
}
