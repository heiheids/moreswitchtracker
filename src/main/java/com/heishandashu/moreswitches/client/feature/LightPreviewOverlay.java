package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.light.ClientHeldLight;
import com.heishandashu.moreswitches.config.SwitchIds;
import com.heishandashu.moreswitches.light.LightFalloff;
import com.heishandashu.moreswitches.light.LightPreviewRule;
import com.heishandashu.moreswitches.light.ScreenProjection;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws the light-level numbers on the HUD.
 *
 * <p>This is deliberately screen-space rather than world-space. Submitting text into the world pass
 * produced nothing visible on the newer render pipeline even though the submissions were being made,
 * so the numbers are projected onto the screen with plain arithmetic and drawn with the ordinary GUI
 * text renderer.</p>
 *
 * <p>Only blocks the simulated source would actually brighten get a number, and only blocks with
 * open space above them, so the marked area equals the area a real torch would change. The nearest
 * few hundred are kept so a wide radius cannot flood the screen or cost frames.</p>
 */
public final class LightPreviewOverlay implements ClientFeature {

    /** Cyan, opaque. */
    private static final int CYAN = 0xFF00E5E5;

    /** Upper bound on numbers drawn in one frame; beyond this the screen is unreadable anyway. */
    private static final int MAX_NUMBERS = 320;

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.LIGHT_PREVIEW, this.active ? "on" : "off");
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.LIGHT_PREVIEW, enabled);
    }

    /** Runs after the vanilla HUD has been laid out, so the numbers sit on top of it. */
    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!this.active || !LightPreviewRule.previewEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        int emission = ClientHeldLight.emission(minecraft);
        if (emission <= 0) {
            return;
        }
        Vec3 centre = ClientHeldLight.centre(minecraft);
        if (centre == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        DeltaTracker delta = event.getPartialTick();
        float partialTick = delta.getGameTimeDeltaPartialTick(false);

        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        Vec3 look = minecraft.player.getViewVector(partialTick);
        double fov = minecraft.options.fov().get();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        List<Entry> entries = collect(minecraft, centre, emission, eye, look, fov, width, height);
        for (Entry entry : entries) {
            String text = Integer.toString(entry.level());
            int textWidth = minecraft.font.width(text);
            graphics.drawString(minecraft.font, text,
                    (int) Math.round(entry.point().x()) - textWidth / 2,
                    (int) Math.round(entry.point().y()), CYAN);
        }
    }

    /** Gathers the blocks worth labelling, nearest first, capped at {@link #MAX_NUMBERS}. */
    private static List<Entry> collect(Minecraft minecraft, Vec3 centre, int emission,
                                       Vec3 eye, Vec3 look, double fov, int width, int height) {
        int reach = LightFalloff.reach(emission);
        BlockPos origin = BlockPos.containing(centre);
        List<Entry> entries = new ArrayList<>();

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!isLabelled(minecraft, pos)) {
                        continue;
                    }
                    int simulated = ClientHeldLight.levelAt(minecraft, pos, emission, centre);
                    if (simulated <= 0) {
                        continue;
                    }
                    // A spot the world already lights at least as brightly gains nothing, so a
                    // number there would be a lie.
                    if (minecraft.level.getBrightness(LightLayer.BLOCK, pos) >= simulated) {
                        continue;
                    }
                    ScreenProjection.Point point = ScreenProjection.project(
                            eye.x, eye.y, eye.z, look.x, look.y, look.z,
                            pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            fov, width, height);
                    if (!ScreenProjection.onScreen(point, width, height, 24)) {
                        continue;
                    }
                    entries.add(new Entry(point, simulated,
                            eye.distanceToSqr(Vec3.atCenterOf(pos))));
                }
            }
        }

        entries.sort(Comparator.comparingDouble(Entry::distanceSqr));
        return entries.size() > MAX_NUMBERS ? entries.subList(0, MAX_NUMBERS) : entries;
    }

    /** A solid block with open space above it, i.e. one whose top face a player could see. */
    private static boolean isLabelled(Minecraft minecraft, BlockPos pos) {
        return minecraft.level.isLoaded(pos)
                && !minecraft.level.getBlockState(pos).isAir()
                && minecraft.level.getBlockState(pos.above()).isAir();
    }

    private record Entry(ScreenProjection.Point point, int level, double distanceSqr) {
    }
}
