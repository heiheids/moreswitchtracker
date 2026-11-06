package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.fullscreen.FullscreenKeyRule;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ignores the fullscreen toggle while the switch is on.
 *
 * <p>Minecraft 1.21.1 handles F11 by calling {@code Window.toggleFullScreen()}: the key handling and
 * the button in the video settings both go through it, so blocking here covers the key and the
 * button together. The newer client has a {@code Minecraft.toggleFullscreen} method instead, which is
 * why this lives on {@code Window} for this version.</p>
 */
@Mixin(Window.class)
public class MixinMinecraftFullscreen {

    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockFullscreenKey(CallbackInfo cir) {
        if (FullscreenKeyRule.blocksToggle()) {
            cir.cancel();
        }
    }
}
