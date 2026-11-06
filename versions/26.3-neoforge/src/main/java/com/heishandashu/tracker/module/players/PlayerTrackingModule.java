package com.heishandashu.tracker.module.players;

import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.module.TrackerModule;
import com.heishandashu.tracker.module.TrackerModules;
import com.heishandashu.tracker.net.PlayerTrackNetworking;
import com.heishandashu.tracker.net.TrackerNetwork;

/**
 * 玩家追踪 / Player Tracking -- highlight another player and follow them with a reticle.
 *
 * <p>Picked from the {@code @} mention list in chat; the highlight lasts ten minutes and shows the
 * distance. On the client the reticle follows the player's entity while it is in range and the
 * server-pushed position after that, so it stays visible anywhere in the same dimension.</p>
 *
 * <p>Server side lives here ({@link PlayerWatchService}); the chat list and the reticle live in
 * {@code src/main/java/.../client/module/players/}.</p>
 */
public final class PlayerTrackingModule implements TrackerModule {

    @Override
    public String id() {
        return ModuleIds.PLAYER_TRACKING;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    /** Read on both sides, so flipping the switch takes effect immediately. */
    public static boolean isActive() {
        return TrackerModules.isEnabled(ModuleIds.PLAYER_TRACKING);
    }

    @Override
    public void registerCommon() {
        // Both sides must know both channels before anything is sent on them. The handler for the
        // client -> server half is attached here, because NeoForge wants it at registration time.
        TrackerNetwork.serverbound(PlayerTrackNetworking.WatchPayload.TYPE,
                PlayerTrackNetworking.WatchPayload.STREAM_CODEC, PlayerWatchService::handleWatch);
        TrackerNetwork.clientbound(PlayerTrackNetworking.PositionPayload.TYPE,
                PlayerTrackNetworking.PositionPayload.STREAM_CODEC);
    }

    @Override
    public void registerServer() {
        PlayerWatchService.init();
    }
}
