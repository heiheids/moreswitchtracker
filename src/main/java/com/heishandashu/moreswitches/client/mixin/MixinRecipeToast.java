package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.quiet.QuietRules;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the "new recipe unlocked" reminder from appearing.
 *
 * <p>This reminder is not part of the tutorial: it is a separate toast raised whenever the server
 * reports a recipe the player can now craft, so suppressing the tutorial alone leaves it on screen.
 * The switch that turns the tutorial off is documented as covering both, and this is the piece that
 * makes that true.</p>
 *
 * <p>Only the pop-up is dropped. The recipe itself is still registered and still appears in the
 * recipe book; the player simply is not interrupted to be told about it.</p>
 */
@Mixin(RecipeToast.class)
public class MixinRecipeToast {

    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    private static void moreswitches$suppressRecipeToast(ToastComponent toastComponent,
                                                         RecipeHolder<?> display,
                                                         CallbackInfo cir) {
        if (QuietRules.suppressTutorial()) {
            cir.cancel();
        }
    }
}
