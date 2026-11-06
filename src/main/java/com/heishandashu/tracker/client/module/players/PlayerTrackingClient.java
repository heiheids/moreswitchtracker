package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.PlayerTrackNetworking;

/**
 * Client half of 玩家追踪 / Player Tracking.
 *
 * <p>Wiring only: the mention list decides who ({@link MentionInput} + {@link AtMentionHandler}),
 * {@link PlayerHighlightState} remembers who and for how long, and
 * {@link PlayerHighlightRenderer} draws it. This class keeps the server told about the watch set
 * so that far-away highlights keep working.</p>
 */
public final class PlayerTrackingClient implements ClientTrackerModule {

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.PLAYER_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        com.heishandashu.tracker.compat.ClientEvents.addTick(
                () -> pushWatchList(net.minecraft.client.Minecraft.getInstance()));

        PlayerHighlightRenderer.register();
        PlayerHighlightHud.register();
    }

    /** Tells the server which players we want positions for, whenever that set changes. */
    private static void pushWatchList(net.minecraft.client.Minecraft minecraft) {
        if (!PlayerHighlightState.watchDirty() || minecraft.getConnection() == null || minecraft.level == null) {
            return;
        }
        // On a server without the mod the send fails at the channel level; NeoForge throws rather
        // than returning false, so the whole send is guarded and the feature quietly falls back to
        // entity-range tracking there.
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new PlayerTrackNetworking.WatchPayload(PlayerHighlightState.watchIds()));
        } catch (Throwable ignored) {
            // no tracker channel on this server -- stay silent and keep tracking what we can see
        }
        PlayerHighlightState.watchSent();
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            PlayerHighlightState.clear();
        }
    }
}
