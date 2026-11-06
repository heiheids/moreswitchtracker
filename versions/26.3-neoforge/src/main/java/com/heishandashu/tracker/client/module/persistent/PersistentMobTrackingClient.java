package com.heishandashu.tracker.client.module.persistent;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.PickedMobNetworking;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client half of 不消失怪物追踪 / persistence-required mob tracking.
 *
 * <p>Receives the list, and turns a typed {@code #00001} in chat into a ten-minute highlight. A
 * message that is nothing but an id is treated as that request and not sent as chat -- only when
 * it is *exactly* an id, so ordinary chat is untouched.</p>
 */
public final class PersistentMobTrackingClient implements ClientTrackerModule {

    /** A message that is nothing but an id, e.g. "#00007". */
    private static final Pattern ID_ONLY = Pattern.compile("#(\\d{1,5})");

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.PERSISTENT_MOB_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        TrackerClientEvents.receive(PickedMobNetworking.PickedMobListPayload.TYPE, PickedMobHighlightState::accept);

        TrackerClientEvents.onClientTick(PickedMobHighlightState::tick);
        PickedMobHighlightRenderer.register();
    }

    /**
     * Called for every outgoing chat message.
     *
     * @return true when the message was an id and has been handled, so it must not be sent
     */
    public static boolean onChatSent(String message) {
        if (!enabled() || message == null) {
            return false;
        }
        Matcher matcher = ID_ONLY.matcher(message.trim());
        if (!matcher.matches()) {
            return false;
        }
        int id = Integer.parseInt(matcher.group(1));
        Minecraft minecraft = Minecraft.getInstance();
        String label = String.format("#%05d", id);
        if (PickedMobHighlightState.highlight(id)) {
            feedback(minecraft, Component.translatable("tracker.picked.highlighted", label));
        } else {
            feedback(minecraft, Component.translatable("tracker.picked.unknown_id", label));
        }
        return true;
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.PERSISTENT_MOB_TRACKING);
    }

    private static void feedback(Minecraft minecraft, Component message) {
        minecraft.gui.hud.getChat().addClientSystemMessage(message);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            PickedMobHighlightState.clear();
        }
    }
}
