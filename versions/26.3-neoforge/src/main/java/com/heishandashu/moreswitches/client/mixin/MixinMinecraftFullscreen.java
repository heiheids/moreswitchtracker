package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.fullscreen.FullscreenKeyRule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ignores F11 while the switch is on.
 *
 * <p>26.3 moved the toggle from {@code Window.toggleFullScreen()} to the private
 * {@code Minecraft.toggleFullscreen()}; both the F11 path and the video-settings button still go
 * through it.</p>
 */
@Mixin(Minecraft.class)
public class MixinMinecraftFullscreen {

    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$blockFullscreenKey(CallbackInfo cir) {
        if (FullscreenKeyRule.blocksToggle()) {
            cir.cancel();
        }
    }
}
