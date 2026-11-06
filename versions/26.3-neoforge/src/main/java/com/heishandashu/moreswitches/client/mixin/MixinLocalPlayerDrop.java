package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.drop.DropKeyRule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the drop key from throwing an item away.
 *
 * <p>26.3 removed {@code LocalPlayer.drop(boolean)}; the key path now calls
 * {@code MultiPlayerGameMode.dropItem(LocalPlayer, boolean)}. Cancelling at HEAD prevents both the
 * inventory mutation and the packet.</p>
 */
@Mixin(MultiPlayerGameMode.class)
public class MixinLocalPlayerDrop {

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockDrop(LocalPlayer player, boolean all, CallbackInfo cir) {
        if (DropKeyRule.blocksDrop()) {
            cir.cancel();
        }
    }
}
