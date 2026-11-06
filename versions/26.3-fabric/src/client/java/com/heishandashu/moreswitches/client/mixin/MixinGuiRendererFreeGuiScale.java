package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the GUI with a fractional scale.
 *
 * <p>Two spots convert the integer scale into pixels: the ortho projection
 * ({@code width / guiScale}) and the scissor rectangle ({@code rect * guiScale}). Both are patched to
 * use {@link FreeGuiScale#scale()} instead. Everything else -- layout sizes, mouse mapping, text
 * positions -- already flows through {@code Window.getGuiScaledWidth()}, which the window mixin
 * fills with the fractional value, so no other call site needs to change.</p>
 */
@Mixin(GuiRenderer.class)
public class MixinGuiRendererFreeGuiScale {

    @ModifyArg(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"),
            index = 2)
    private float moreswitches$projectionWidth(float original) {
        if (!FreeGuiScale.active()) {
            return original;
        }
        return Minecraft.getInstance().getWindow().getWidth() / FreeGuiScale.scale();
    }

    @ModifyArg(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"),
            index = 3)
    private float moreswitches$projectionHeight(float original) {
        if (!FreeGuiScale.active()) {
            return original;
        }
        return Minecraft.getInstance().getWindow().getHeight() / FreeGuiScale.scale();
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$fractionalScissor(ScreenRectangle rectangle, RenderPass renderPass,
                                                CallbackInfo cir) {
        if (!FreeGuiScale.active()) {
            return;
        }
        WindowRenderState window =
                Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
        double scale = FreeGuiScale.scale();
        double left = rectangle.left() * scale;
        double top = rectangle.top() * scale;
        double right = Math.min(rectangle.right() * scale, window.width);
        double bottom = Math.min(rectangle.bottom() * scale, window.height);
        renderPass.enableScissor((int) left, window.height - (int) bottom,
                Math.max(0, (int) (right - left)), Math.max(0, (int) (bottom - top)));
        cir.cancel();
    }
}
