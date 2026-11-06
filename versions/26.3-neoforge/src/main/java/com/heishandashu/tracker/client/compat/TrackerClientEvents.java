package com.heishandashu.tracker.client.compat;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client-only counterpart of {@code TrackerEvents}: the loader-shaped door for the client half.
 *
 * <p>Maps the Fabric calls the client modules were written against onto their NeoForge
 * counterparts:</p>
 *
 * <ul>
 *   <li>{@code ClientTickEvents.END_CLIENT_TICK} -&gt; {@link ClientTickEvent.Post}</li>
 *   <li>{@code HudElementRegistry.addLast} -&gt; {@link RegisterGuiLayersEvent#registerAboveAll}</li>
 *   <li>{@code LevelRenderEvents.BEFORE_GIZMOS} -&gt; {@link SubmitCustomGeometryEvent}, wrapped in
 *       {@code LevelRenderer#collectPerFrameRenderThreadGizmos()}</li>
 *   <li>{@code ClientPlayNetworking.registerGlobalReceiver} -&gt;
 *       {@link RegisterClientPayloadHandlersEvent}</li>
 *   <li>{@code ClientPlayNetworking.send} / {@code canSend} -&gt;
 *       {@link ClientPacketDistributor#sendToServer} / {@link NetworkRegistry#hasChannel}</li>
 * </ul>
 *
 * <p><b>Why the gizmo wrapper.</b> 26.3 draws debug geometry through a per-frame
 * {@code SimpleGizmoCollector} that the level renderer drains itself. Vanilla only routes gizmos
 * into the frame while a collector is installed on the calling thread, and NeoForge exposes no
 * "before gizmos" event -- but {@code LevelRenderer#collectPerFrameRenderThreadGizmos()} is public
 * and re-installs the very collector the renderer is about to drain, so opening it for the
 * duration of our drawing puts our gizmos in exactly the right list. NeoForge's
 * {@link SubmitCustomGeometryEvent} fires immediately before that drain, which is the same instant
 * Fabric's {@code BEFORE_GIZMOS} fires.</p>
 */
public final class TrackerClientEvents {

    /** HUD layers, drained when the client-registration event fires. */
    private static final Map<Identifier, GuiLayer> PENDING_LAYERS = new LinkedHashMap<>();

    /** Clientbound receivers, drained when the client-registration event fires. */
    private static final List<Consumer<RegisterClientPayloadHandlersEvent>> PENDING_RECEIVERS = new ArrayList<>();

    /** Through-wall drawing, run once per frame while the collector is open. */
    private static final List<Runnable> GIZMO_DRAWERS = new ArrayList<>();

    private static final List<Runnable> TICK_ACTIONS = new ArrayList<>();

    private TrackerClientEvents() {
    }

    /**
     * Wires the client halves onto the two buses. Called once, from the client bootstrap.
     */
    public static void registerBuses(IEventBus modBus) {
        modBus.addListener(TrackerClientEvents::onRegisterClientPayloadHandlers);
        modBus.addListener(TrackerClientEvents::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(TrackerClientEvents::handleClientTick);
        NeoForge.EVENT_BUS.addListener(TrackerClientEvents::onSubmitGizmos);
    }

    // ------------------------------------------------------------------
    // Registration helpers used by the client modules
    // ------------------------------------------------------------------

    /** Runs once per client tick. */
    public static void onClientTick(Runnable action) {
        TICK_ACTIONS.add(action);
    }

    /** Adds a HUD layer drawn above everything vanilla draws. */
    public static void onHudLayer(Identifier id, GuiLayer layer) {
        if (PENDING_LAYERS.putIfAbsent(id, layer) != null) {
            throw new IllegalStateException("duplicate tracker HUD layer: " + id);
        }
    }

    /**
     * Registers drawing that must land in the level renderer's gizmo collector.
     *
     * <p>The action itself calls the vanilla {@code Gizmos} helpers; it runs during
     * {@link SubmitCustomGeometryEvent}, once per frame.</p>
     */
    public static void onGizmos(Runnable drawing) {
        GIZMO_DRAWERS.add(drawing);
    }

    /** Registers a handler for one server -&gt; client channel. */
    public static <T extends CustomPacketPayload> void receive(
            CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        PENDING_RECEIVERS.add(event -> event.register(type, (payload, context) ->
                // Payload handlers run off the game thread; hop back on before touching state.
                context.enqueueWork(() -> handler.accept(payload))));
    }

    // ------------------------------------------------------------------
    // Sending
    // ------------------------------------------------------------------

    /**
     * True when the server on the other end knows this channel.
     *
     * <p>The Fabric version used {@code ClientPlayNetworking.canSend} to degrade to
     * entity-range tracking on a server without the mod instead of throwing; this is the same
     * question asked of NeoForge's channel registry.</p>
     */
    public static boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && NetworkRegistry.hasChannel(listener, type.id());
    }

    /** client -&gt; server. Check {@link #canSendToServer} first when the server may not have the mod. */
    public static void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    // ------------------------------------------------------------------
    // Bus listeners
    // ------------------------------------------------------------------

    private static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        for (Consumer<RegisterClientPayloadHandlersEvent> receiver : PENDING_RECEIVERS) {
            receiver.accept(event);
        }
        PENDING_RECEIVERS.clear();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        for (Map.Entry<Identifier, GuiLayer> entry : PENDING_LAYERS.entrySet()) {
            event.registerAboveAll(entry.getKey(), entry.getValue());
        }
        PENDING_LAYERS.clear();
    }

    private static void handleClientTick(ClientTickEvent.Post event) {
        for (Runnable action : TICK_ACTIONS) {
            action.run();
        }
    }

    private static void onSubmitGizmos(SubmitCustomGeometryEvent event) {
        if (GIZMO_DRAWERS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // Open the renderer's own collector for the duration of our drawing. Without this the
        // vanilla Gizmos helpers would write into a no-op collector and nothing would appear.
        try (Gizmos.TemporaryCollection ignored = minecraft.levelRenderer.collectPerFrameRenderThreadGizmos()) {
            for (Runnable drawing : GIZMO_DRAWERS) {
                try {
                    drawing.run();
                } catch (Throwable t) {
                    // Never let one feature's drawing take the frame (or the game) down.
                    TrackerMod.LOGGER.warn("[tracker] gizmo drawing failed; skipping this frame", t);
                }
            }
        }
    }
}
