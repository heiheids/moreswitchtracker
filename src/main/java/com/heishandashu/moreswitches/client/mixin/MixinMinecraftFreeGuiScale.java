package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Feeds the free scale into Minecraft's own resize routine.
 *
 * <p>{@code Minecraft.resizeDisplay()} asks the options for an integer scale, lets the window clamp
 * it, and then hands that integer to {@code Window.setGuiScale(double)}. The window itself is
 * perfectly happy with a fractional value: it stores the {@code double} and divides the framebuffer
 * by it, rounding the interface size up. Only the integer coming out of the options stands in the
 * way, so this redirect replaces that single argument and leaves the rest of the routine intact.</p>
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftFreeGuiScale {

    @Redirect(
            method = "resizeDisplay",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"),
            require = 1)
    private void moreswitches$freeGuiScale(com.mojang.blaze3d.platform.Window window, double vanilla) {
        window.setGuiScale(FreeGuiScale.active() ? (double) FreeGuiScale.scale() : vanilla);
    }
}
