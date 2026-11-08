package com.heishandashu.moreswitches.client.healthbar;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.HealthBarCategory;
import com.heishandashu.moreswitches.config.HealthBarDisplay;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.tracker.client.render.ScreenProjection;
import com.heishandashu.tracker.compat.ClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 血条 / overhead health bar for the 1.21.1 build.
 *
 * <p>Same behaviour as the 26.3 sibling: the bar has a size in blocks and is converted to pixels
 * with the projection's own pinhole maths, so it shrinks with distance; entity positions and the
 * camera are interpolated with the frame's partial tick and placed on sub-pixel offsets, so a
 * moving creature does not make the bar stutter.</p>
 *
 * <p>1.21.1 has no {@code HudElementRegistry}, so the pass is registered through the tracker's
 * {@link ClientEvents} HUD hook, which hands over the partial tick directly.</p>
 */
public final class HealthBarOverlay {

    /** Bar width in world blocks at 中 size; the size setting multiplies this. */
    private static final float WORLD_BAR_WIDTH = 1.35F;
    private static final float BAR_ASPECT = 0.11F;
    private static final float TEXT_ASPECT = 0.16F;

    private static final float MIN_BAR_WIDTH = 10.0F;
    private static final float MAX_BAR_WIDTH = 200.0F;
    private static final float MIN_BAR_HEIGHT = 3.0F;
    private static final float MAX_BAR_HEIGHT = 22.0F;
    private static final float MIN_TEXT_SCALE = 0.5F;
    private static final float MAX_TEXT_SCALE = 2.0F;

    private static final double RANGE = 48.0;
    private static final int MAX_BARS = 48;
    private static final double HEAD_GAP = 0.42;

    private static final int BORDER_COLOR = 0xFF0D0404;
    private static final int TRACK_COLOR = 0xFF2A1212;
    private static final int FILL_TOP_COLOR = 0xFFFF3B3B;
    private static final int FILL_BOTTOM_COLOR = 0xFF8C0000;
    private static final int GLOSS_COLOR = 0x55FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static boolean warned;
    private static boolean reported;

    private HealthBarOverlay() {
    }

    public static void register() {
        ClientEvents.addHudPass(HealthBarOverlay::render);
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        ModSettings settings = ModSettings.get();
        HealthBarDisplay display = settings.healthBarDisplay();
        if (display == HealthBarDisplay.OFF) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer self = minecraft.player;
        if (level == null || self == null) {
            return;
        }

        try {
            draw(graphics, minecraft, level, self, settings, display, partialTick);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                MoreSwitchesMod.LOGGER.warn("[{}] health bar overlay could not draw",
                        MoreSwitchesMod.MOD_ID, t);
            }
        }
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, ClientLevel level,
                             LocalPlayer self, ModSettings settings, HealthBarDisplay display,
                             float partialTick) {
        List<LivingEntity> targets = collect(level, self, settings, partialTick);
        if (!reported) {
            reported = true;
            MoreSwitchesMod.LOGGER.info("[{}] health bar: {} creatures in range, display={}, size={}",
                    MoreSwitchesMod.MOD_ID, targets.size(), display.id(),
                    settings.healthBarSize().id());
        }
        if (targets.isEmpty()) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Vec3 eye = self.getEyePosition(partialTick);
        Vec3 forward = self.getViewVector(partialTick);
        double fov = minecraft.options.fov().get();
        Font font = minecraft.font;
        float sizeFactor = settings.healthBarSize().factor();
        float worldWidth = WORLD_BAR_WIDTH * sizeFactor;
        double tanHalfFov = Math.tan(Math.toRadians(fov) / 2.0);

        for (LivingEntity entity : targets) {
            double anchorX = Mth.lerp(partialTick, entity.xo, entity.getX());
            double anchorY = Mth.lerp(partialTick, entity.yo, entity.getY())
                    + entity.getBbHeight() + HEAD_GAP;
            double anchorZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
            Vec3 anchor = new Vec3(anchorX, anchorY, anchorZ);

            ScreenProjection.Projected projected =
                    ScreenProjection.project(eye, forward, fov, width, height, anchor, 0.0);
            if (!projected.onScreen() || projected.distance() > RANGE) {
                continue;
            }

            double pixelsPerBlock = (height / 2.0) / (tanHalfFov * Math.max(0.25, projected.distance()));
            float barWidth = clamp((float) (worldWidth * pixelsPerBlock),
                    MIN_BAR_WIDTH, MAX_BAR_WIDTH);
            float barHeight = clamp(barWidth * BAR_ASPECT, MIN_BAR_HEIGHT, MAX_BAR_HEIGHT);
            float textScale = clamp((float) (worldWidth * TEXT_ASPECT * pixelsPerBlock
                    / Math.max(1, font.lineHeight)), MIN_TEXT_SCALE, MAX_TEXT_SCALE);

            float exactX = (float) projected.screenX();
            float exactY = (float) projected.screenY();
            int baseX = (int) Math.floor(exactX);
            int baseY = (int) Math.floor(exactY);

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(exactX - baseX, exactY - baseY, 0.0F);

            int barPixels = Math.round(barWidth);
            int barPixelsHigh = Math.round(barHeight);
            if (display.showsBar()) {
                drawBar(graphics, entity, baseX, baseY, barPixels, barPixelsHigh);
            }
            if (display.showsNumbers()) {
                int textTop = display.showsBar()
                        ? baseY - barPixelsHigh / 2 - Math.round(font.lineHeight * textScale) - 1
                        : baseY - Math.round(font.lineHeight * textScale) / 2;
                drawNumbers(graphics, font, entity, baseX, textTop, textScale);
            }

            pose.popPose();
        }
    }

    private static void drawBar(GuiGraphics graphics, LivingEntity entity,
                                int centerX, int centerY, int barWidth, int barHeight) {
        int left = centerX - barWidth / 2;
        int top = centerY - barHeight / 2;
        int right = left + barWidth;
        int bottom = top + barHeight;

        float max = Math.max(1.0F, entity.getMaxHealth());
        float ratio = Math.max(0.0F, Math.min(1.0F, entity.getHealth() / max));
        int filled = Math.round(barWidth * ratio);

        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER_COLOR);
        graphics.fill(left, top, right, bottom, TRACK_COLOR);
        if (filled > 0) {
            graphics.fillGradient(left, top, left + filled, bottom,
                    FILL_TOP_COLOR, FILL_BOTTOM_COLOR);
            if (barHeight >= 4) {
                graphics.fill(left, top, left + filled, top + 1, GLOSS_COLOR);
            }
        }
    }

    private static void drawNumbers(GuiGraphics graphics, Font font, LivingEntity entity,
                                    int centerX, int textTop, float scale) {
        String label = formatHealth(entity.getHealth()) + "/" + formatHealth(entity.getMaxHealth());
        float scaledWidth = font.width(label) * scale;
        float x = centerX - scaledWidth / 2.0F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, textTop, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(font, label, 0, 0, TEXT_COLOR, true);
        pose.popPose();
    }

    private static String formatHealth(float value) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05F) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static List<LivingEntity> collect(ClientLevel level, LocalPlayer self,
                                              ModSettings settings, float partialTick) {
        Vec3 eye = self.getEyePosition(partialTick);
        List<LivingEntity> found = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == self) {
                continue;
            }
            if (!living.isAlive() || living.isInvisible()) {
                continue;
            }
            HealthBarCategory category = HealthBarCategoryResolver.of(living);
            if (category == null || !settings.isHealthBarEnabled(category)) {
                continue;
            }
            double x = Mth.lerp(partialTick, living.xo, living.getX());
            double y = Mth.lerp(partialTick, living.yo, living.getY());
            double z = Mth.lerp(partialTick, living.zo, living.getZ());
            if (eye.distanceToSqr(x, y, z) > RANGE * RANGE) {
                continue;
            }
            found.add(living);
        }

        if (found.size() > MAX_BARS) {
            found.sort(Comparator.comparingDouble(entity -> eye.distanceToSqr(
                    Mth.lerp(partialTick, entity.xo, entity.getX()),
                    Mth.lerp(partialTick, entity.yo, entity.getY()),
                    Mth.lerp(partialTick, entity.zo, entity.getZ()))));
            return new ArrayList<>(found.subList(0, MAX_BARS));
        }
        return found;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
