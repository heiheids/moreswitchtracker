package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops the "new recipe unlocked" reminder from appearing. */
@Mixin(RecipeToast.class)
public class MixinRecipeToast {

    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    private static void moreswitches$suppressRecipeToast(ToastManager toastManager,
                                                         RecipeDisplay display,
                                                         CallbackInfo cir) {
        if (QuietRules.suppressTutorial()) {
            cir.cancel();
        }
    }
}
