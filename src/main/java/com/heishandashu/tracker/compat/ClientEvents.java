package com.heishandashu.tracker.compat;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side event glue: the 1.21.1 NeoForge equivalent of the Fabric client callbacks.
 *
 * <p>Mirrors {@link ServerEvents} on purpose. The feature renderers register a world pass and a
 * HUD pass here instead of calling into Fabric's event registry, and the HUD layers all share one
 * injection point because 1.21.1 NeoForge registers GUI layers by name rather than by arbitrary
 * callback.</p>
 */
@EventBusSubscriber(modid = TrackerMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    /** World-space passes, run from the level render stage event. */
    private static final List<WorldPass> WORLD_PASSES = new ArrayList<>();
    /** HUD passes: (gui graphics, partial tick). */
    private static final List<HudPass> HUD_PASSES = new ArrayList<>();
    /** Client tick listeners. */
    private static final List<Runnable> TICKS = new ArrayList<>();
    /** Key mappings to register, filled by the keybind class before the register event fires. */
    private static final List<net.minecraft.client.KeyMapping> KEY_MAPPINGS = new ArrayList<>();

    private ClientEvents() {
    }

    @FunctionalInterface
    public interface WorldPass {
        /**
         * @param pose    the camera-aligned pose stack from the render event
         * @param buffers the buffer source to submit geometry into
         */
        void draw(com.mojang.blaze3d.vertex.PoseStack pose,
                  net.minecraft.client.renderer.MultiBufferSource buffers);
    }

    @FunctionalInterface
    public interface HudPass {
        void draw(net.minecraft.client.gui.GuiGraphics graphics, float partialTick);
    }

    // ---- registration used by the feature modules -------------------------------------------

    public static void addWorldPass(WorldPass pass) {
        WORLD_PASSES.add(pass);
    }

    public static void addHudPass(HudPass pass) {
        HUD_PASSES.add(pass);
    }

    public static void addTick(Runnable listener) {
        TICKS.add(listener);
    }

    public static void addKeyMapping(net.minecraft.client.KeyMapping mapping) {
        KEY_MAPPINGS.add(mapping);
    }

    public static List<WorldPass> worldPasses() {
        return WORLD_PASSES;
    }

    // ---- mod-bus events ----------------------------------------------------------------------

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (net.minecraft.client.KeyMapping mapping : KEY_MAPPINGS) {
            event.register(mapping);
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // One layer draws every feature's HUD elements: 1.21.1 registers layers by name, and
        // giving each feature its own layer would mean ten registration calls for no benefit.
        event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TrackerMod.MOD_ID, "hud"),
                (graphics, delta) -> {
                    float partialTick = delta.getGameTimeDeltaPartialTick(false);
                    for (HudPass pass : HUD_PASSES) {
                        pass.draw(graphics, partialTick);
                    }
                });
    }

    /** Exposed so the client initialiser can drive ticks from the game bus. */
    @EventBusSubscriber(modid = TrackerMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {

        private GameBus() {
        }

        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            for (Runnable listener : TICKS) {
                listener.run();
            }
        }
    }
}
