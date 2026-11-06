package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the window hold a fractional GUI scale.
 *
 * <p>{@code Window.setGuiScale(int)} stores the scale and rounds the scaled size up. With a free
 * scale we keep the same contract but do the division in {@code double}, so a 2.5x scale yields
 * {@code ceil(width / 2.5)} GUI units instead of {@code ceil(width / 2)}.</p>
 *
 * <p>The stored {@code guiScale} field stays an int (other vanilla code reads it), so it gets the
 * rounded value; the float lives in {@link FreeGuiScale} and is what the renderer uses.</p>
 */
@Mixin(Window.class)
public class MixinWindowFreeGuiScale {

    @Shadow
    private int guiScale;

    @Shadow
    private int guiScaledWidth;

    @Shadow
    private int guiScaledHeight;

    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Inject(method = "setGuiScale", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$freeScale(int vanillaScale, CallbackInfo cir) {
        if (!FreeGuiScale.active()) {
            return;
        }
        float exact = FreeGuiScale.scale();
        this.guiScale = Math.max(1, Math.round(exact));
        this.guiScaledWidth = (int) Math.ceil(this.framebufferWidth / (double) exact);
        this.guiScaledHeight = (int) Math.ceil(this.framebufferHeight / (double) exact);
        cir.cancel();
    }
}
