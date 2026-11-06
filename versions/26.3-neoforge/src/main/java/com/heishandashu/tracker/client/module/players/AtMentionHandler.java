package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides <em>who</em> gets highlighted. Never touches the chat input.
 *
 * <p>Two routes reach it, both of them ordinary chat use:</p>
 * <ul>
 *   <li>a sent message containing {@code @Name} -- the "type it out and press Enter" route</li>
 *   <li>picking a name from the {@code @} list, which highlights without sending anything</li>
 * </ul>
 *
 * <p>How the reticle is drawn is {@link PlayerHighlightRenderer}'s business, not this class's.</p>
 */
public final class AtMentionHandler {

    /** {@code @Name} -- Minecraft names are 3-16 of [A-Za-z0-9_]. */
    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_]{1,16})");

    private AtMentionHandler() {
    }

    /** Called for every chat message the player sends. */
    public static void onChatSent(String message) {
        if (!TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING) || message == null) {
            return;
        }
        Matcher matcher = MENTION.matcher(message);
        while (matcher.find()) {
            highlightNow(matcher.group(1));
        }
    }

    /** Highlights an online player by name, with feedback either way. */
    public static void highlightNow(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || name == null || name.isEmpty()) {
            return;
        }
        PlayerInfo info = lookup(name);
        if (info == null) {
            feedback(minecraft, Component.translatable("tracker.players.unknown", name));
            return;
        }
        String realName = info.getProfile().name();
        PlayerHighlightState.highlight(info.getProfile().id(), realName);
        feedback(minecraft, Component.translatable("tracker.players.highlighted", realName));
    }

    private static PlayerInfo lookup(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? null : connection.getPlayerInfoIgnoreCase(name);
    }

    /** Client-side notice: goes into the local chat log, never to the server. */
    private static void feedback(Minecraft minecraft, Component message) {
        minecraft.gui.hud.getChat().addClientSystemMessage(message);
    }
}
