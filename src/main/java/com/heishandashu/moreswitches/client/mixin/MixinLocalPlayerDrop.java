package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.drop.DropKeyRule;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the drop key from throwing an item away.
 *
 * <p>On 1.21.1 the Q key ends up in {@code LocalPlayer.drop(boolean)}: the boolean says whether the
 * whole stack should go, so this single method is the whole feature — the plain press and Ctrl+Q
 * both route through it. Cancelling at <code>HEAD</code> returns {@code false} to the caller, which
 * reads as "nothing was dropped", and nothing is removed from the inventory or sent to the server.</p>
 *
 * <p>This lives on {@code LocalPlayer} rather than on {@code MultiPlayerGameMode} because 1.21.1 has
 * no {@code dropItem} method there; putting it on the wrong class made Mixin fail to find its target
 * and crash the game when the world finished loading.</p>
 *
 * <p>The decision itself is {@link DropKeyRule}, so the rule can be unit tested without the game.</p>
 */
@Mixin(LocalPlayer.class)
public class MixinLocalPlayerDrop {

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (DropKeyRule.blocksDrop()) {
            // false means "the drop did not happen", which is exactly what the caller expects when
            // an item could not be thrown.
            cir.setReturnValue(false);
        }
    }
}
