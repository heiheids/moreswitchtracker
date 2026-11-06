package com.heishandashu.tracker.client.module.relations;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.RelationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client wiring for the ray features.
 *
 * <p>Registered once, under the villager switch, because the three features share one payload and
 * one renderer; the renderer consults each switch itself, so a kind whose switch is off draws
 * nothing. The other two ids therefore need no client module of their own.</p>
 */
public final class RelationTrackingClient implements ClientTrackerModule {

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.VILLAGER_POI_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;
        ClientPlayNetworking.registerGlobalReceiver(RelationNetworking.RelationListPayload.TYPE,
                (payload, context) -> context.client().execute(() -> RelationState.accept(payload)));
        RelationRenderer.register();
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        // Any of the three switches may have changed; the renderer filters by kind anyway, so the
        // only thing worth doing here is dropping stale lines when everything is off.
        if (!enabled
                && !com.heishandashu.tracker.module.relations.RelationModules.zombieAggroActive()
                && !com.heishandashu.tracker.module.relations.RelationModules.dragonAggroActive()) {
            RelationState.clear();
        }
    }
}
