package com.heishandashu.tracker.client.module.traders;

import com.heishandashu.tracker.client.module.ClientTrackerModule;
import com.heishandashu.tracker.module.ModuleIds;
import com.heishandashu.tracker.net.TraderNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Client half of 流浪商人追踪 / wandering trader tracking.
 *
 * <p>Wiring only: {@link TraderState} remembers what the server sent and which traders were
 * highlighted, {@link TraderOverlay} draws them. The clickable coordinate in chat runs
 * {@code /tracker trader &lt;uuid&gt;}, which lands in {@link #highlight}.</p>
 */
public final class TraderTrackingClient implements ClientTrackerModule {

    private boolean registered;

    @Override
    public String id() {
        return ModuleIds.WANDERING_TRADER_TRACKING;
    }

    @Override
    public void registerClient() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        ClientPlayNetworking.registerGlobalReceiver(TraderNetworking.TraderListPayload.TYPE,
                (payload, context) -> context.client().execute(() -> TraderState.accept(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(client -> TraderState.tick());
        TraderOverlay.register();
    }

    /** Called by the {@code /tracker trader} client command. */
    public static int highlight(String rawUuid) {
        Minecraft minecraft = Minecraft.getInstance();
        UUID uuid;
        try {
            uuid = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            feedback(minecraft, Component.translatable("tracker.trader.not_found"));
            return 0;
        }
        if (TraderState.highlight(uuid)) {
            feedback(minecraft, Component.translatable("tracker.trader.highlighted"));
            return 1;
        }
        feedback(minecraft, Component.translatable("tracker.trader.not_found"));
        return 0;
    }

    private static void feedback(Minecraft minecraft, Component message) {
        minecraft.gui.hud.getChat().addClientSystemMessage(message);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (!enabled) {
            TraderState.clear();
        }
    }
}
