package com.heishandashu.tracker.client.mixin;

import com.heishandashu.tracker.client.module.players.MentionInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the {@code @} mention list on the chat screen.
 *
 * <p>Three thin hooks, all of them delegating immediately: draw the list over the render, and give
 * it first refusal on the up/down/tab keys and on clicks. Nothing here intercepts the {@code @}
 * character itself -- it is typed into the chat box like any other character, and the list is only
 * a description of what could be completed.</p>
 */
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    // require = 1 on purpose: these hooks are the feature. A silently missing hook (the old
    // require = 0) showed up as "the list draws but Tab does nothing", which is far harder to
    // diagnose than a failed launch naming the target.
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 1)
    private void tracker$drawMentionList(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                         float partialTick, CallbackInfo ci) {
        MentionInput.draw((Screen) (Object) this, graphics, mouseX, mouseY);
    }

    // keyPressed and mouseClicked both return boolean, so both need CallbackInfoReturnable.
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 1)
    private void tracker$mentionKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (MentionInput.keyPressed((Screen) (Object) this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void tracker$mentionClick(MouseButtonEvent event, boolean doubleClick,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (MentionInput.mouseClicked((Screen) (Object) this, event)) {
            cir.setReturnValue(true);
        }
    }
}
