package com.heishandashu.tracker.net;

/**
 * Client-side fan-out for incoming payloads.
 *
 * <p>On Fabric each feature registered its own receiver on the client. NeoForge keeps the handler
 * on the registrar (a common-side class), so the handler needs one place to hand the payload to
 * the client-only code. Everything in here is only ever invoked on a physical client: NeoForge
 * never delivers a {@code playToClient} payload to a dedicated server, so the client classes are
 * safe to name here.</p>
 *
 * <p>Each method is a thin forwarder on purpose — the actual state lives in the feature packages,
 * which is what keeps this class from growing into a second, shadow copy of them.</p>
 */
public final class ClientPayloadSink {

    private ClientPayloadSink() {
    }

    public static void legacy(DtpNetworking.LegacySyncPayload payload) {
        com.heishandashu.tracker.client.module.legacy.LegacyHighlightState.accept(payload);
    }

    public static void positions(PlayerTrackNetworking.PositionPayload payload) {
        com.heishandashu.tracker.client.module.players.PlayerHighlightState.acceptPositions(payload);
    }

    public static void traders(TraderNetworking.TraderListPayload payload) {
        com.heishandashu.tracker.client.module.traders.TraderState.accept(payload);
    }

    public static void pickedMobs(PickedMobNetworking.PickedMobListPayload payload) {
        com.heishandashu.tracker.client.module.persistent.PickedMobHighlightState.accept(payload);
    }

    public static void lag(LagNetworking.LagListPayload payload) {
        com.heishandashu.tracker.client.module.lag.LagState.accept(payload);
    }

    public static void relations(RelationNetworking.RelationListPayload payload) {
        com.heishandashu.tracker.client.module.relations.RelationState.accept(payload);
    }
}
