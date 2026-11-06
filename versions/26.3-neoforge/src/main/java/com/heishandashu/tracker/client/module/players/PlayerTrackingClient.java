package com.heishandashu.tracker.client.module.players;

import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.PlayerTrackNetworking;
import net.minecraft.client.Minecraft;

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

        TrackerClientEvents.receive(PlayerTrackNetworking.PositionPayload.TYPE, PlayerHighlightState::acceptPositions);

        TrackerClientEvents.onClientTick(() -> {
            PlayerHighlightState.tick();
            pushWatchList(Minecraft.getInstance());
        });

        PlayerHighlightRenderer.register();
        PlayerHighlightHud.register();
    }

    /** Tells the server which players we want positions for, whenever that set changes. */
    private static void pushWatchList(Minecraft minecraft) {
        if (!PlayerHighlightState.watchDirty() || minecraft.getConnection() == null || minecraft.level == null) {
            return;
        }
        // canSend() is false on a server without the mod, so the feature quietly falls back to
        // entity-range tracking there instead of throwing.
        if (!TrackerClientEvents.canSendToServer(PlayerTrackNetworking.WatchPayload.TYPE)) {
            PlayerHighlightState.watchSent();
            return;
        }
        TrackerClientEvents.sendToServer(new PlayerTrackNetworking.WatchPayload(PlayerHighlightState.watchIds()));
        PlayerHighlightState.watchSent();
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            PlayerHighlightState.clear();
        }
    }
}
