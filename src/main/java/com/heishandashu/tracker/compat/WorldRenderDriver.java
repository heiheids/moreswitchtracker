package com.heishandashu.tracker.compat;

import com.heishandashu.tracker.TrackerMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The one place world-space highlighters are driven from.
 *
 * <p>26.3 fires {@code LevelRenderEvents.BEFORE_GIZMOS} and every feature adds gizmos. 1.21.1 has
 * no gizmo API, so each feature draws its own geometry through {@code WorldPainter}; this class
 * picks the render stage and hands the pass the pose stack and buffer source it needs.</p>
 *
 * <p>{@code AFTER_ENTITIES} is chosen because it runs after solid terrain and entities but before
 * the translucent pass: draws land on top of the world's opaque geometry, which is what the
 * through-wall channel wants.</p>
 */
@EventBusSubscriber(modid = TrackerMod.MOD_ID, value = Dist.CLIENT)
public final class WorldRenderDriver {

    private WorldRenderDriver() {
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ClientEvents.worldPasses().isEmpty()) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        try {
            for (ClientEvents.WorldPass pass : ClientEvents.worldPasses()) {
                pass.draw(pose, buffers);
            }
        } catch (Throwable t) {
            // A highlight that fails to draw must never break the frame for everything else.
            warnOnce(t);
        }
    }

    private static boolean warned;

    private static void warnOnce(Throwable t) {
        if (warned) {
            return;
        }
        warned = true;
        TrackerMod.LOGGER.warn("[tracker] could not draw world highlights", t);
    }
}
