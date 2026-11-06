package com.heishandashu.tracker.client.module.lag;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.LagNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client half of 卡顿来源追踪 / lag source tracking.
 *
 * <p>Receives the hot-spot list the server measured, prints it on demand ({@code /tracker lag}),
 * and turns a typed {@code #L0001} in chat into a ten-minute highlight of that place. A message
 * that is nothing but such an id is treated as the request and is not sent as chat.</p>
 */
public final class LagSourceTrackingClient implements ClientTrackerModule {

    /** A message that is nothing but a lag id, e.g. "#L0007". The L keeps it apart from mob ids. */
    private static final Pattern ID_ONLY = Pattern.compile("#[Ll](\\d{1,4})");

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.LAG_SOURCE_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        LagOverlay.register();
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
        highlight(id);
        return true;
    }

    /** Called by the clickable chat id and by the {@code /tracker lag highlight} command. */
    public static int highlight(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (LagState.highlight(id)) {
            feedback(minecraft, Component.translatable("tracker.lag.highlighted", LagState.label(id)));
            return 1;
        }
        feedback(minecraft, Component.translatable("tracker.lag.unknown_id", LagState.label(id)));
        return 0;
    }

    /** Prints the last measured hot spots, in the client's own language. */
    public static int printList() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LagState.hasSpots()) {
            feedback(minecraft, Component.translatable("tracker.lag.empty"));
            return 0;
        }
        feedback(minecraft, Component.translatable("tracker.lag.header_manual"));
        for (LagNetworking.LagSnap snap : LagState.spots()) {
            feedback(minecraft, Component.translatable("tracker.lag.line",
                    LagState.label(snap.id()),
                    LagState.kind(snap.kind()),
                    Component.literal(LagState.displayName(snap)),
                    Integer.toString(snap.count()),
                    Component.literal(LagState.dimension(snap.dimension())),
                    Component.literal(String.format("%.0f, %.0f, %.0f", snap.x(), snap.y(), snap.z())),
                    String.format("%.2f", snap.millisPerTick()),
                    String.format("%.1f", snap.share())));
        }
        feedback(minecraft, Component.translatable("tracker.lag.hint_manual").withStyle(
                net.minecraft.ChatFormatting.GRAY));
        return 1;
    }

    private static boolean enabled() {
        return TrackerModules.isEnabled(ModuleIds.LAG_SOURCE_TRACKING);
    }

    private static void feedback(Minecraft minecraft, Component message) {
        com.heishandashu.tracker.compat.ClientChat.add(message);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            LagState.clear();
        }
    }
}
