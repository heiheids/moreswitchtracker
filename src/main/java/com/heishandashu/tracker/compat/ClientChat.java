package com.heishandashu.tracker.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side chat output that never reaches the server.
 *
 * <p>26.3 exposes {@code minecraft.gui.hud.getChat().addClientSystemMessage(...)}. 1.21.1 has no
 * such method; the equivalent is to hand a system message to the local player, which stays on
 * this client. Keeping it behind one method means the feature code does not care which generation
 * it is running on.</p>
 */
public final class ClientChat {

    private ClientChat() {
    }

    public static void add(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return; // no world yet; drop rather than crash
        }
        minecraft.player.displayClientMessage(message, false);
    }
}
