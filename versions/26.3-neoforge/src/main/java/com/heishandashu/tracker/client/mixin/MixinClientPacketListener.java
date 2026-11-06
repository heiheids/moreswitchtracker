package com.heishandashu.tracker.client.mixin;

import com.heishandashu.tracker.client.module.players.AtMentionHandler;
import com.heishandashu.tracker.client.module.lag.LagSourceTrackingClient;
import com.heishandashu.tracker.client.module.persistent.PersistentMobTrackingClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees chat messages on their way out, so a sent {@code @Name} starts the highlight.
 *
 * <p>Hooking the outgoing chat means both routes work: picking a name from the menu and simply
 * typing {@code @Name} by hand.</p>
 */
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true, require = 1)
    private void tracker$onSendChat(String message, CallbackInfo ci) {
        AtMentionHandler.onChatSent(message);
        // A message that is nothing but an id is a request, not chat: swallow it.
        if (PersistentMobTrackingClient.onChatSent(message)
                || LagSourceTrackingClient.onChatSent(message)) {
            ci.cancel();
        }
    }
}
