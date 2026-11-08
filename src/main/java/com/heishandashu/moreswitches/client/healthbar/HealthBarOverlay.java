package com.heishandashu.moreswitches.client.healthbar;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.HealthBarCategory;
import com.heishandashu.moreswitches.config.HealthBarDisplay;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.tracker.client.diagnostic.RenderWatchdog;
import com.heishandashu.tracker.compat.ClientEvents;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 血条 / overhead health bar for the 1.21.1 build.
 *
 * <p>The bar has to land on the creature exactly, so it is projected with the same matrix the world
 * was drawn with instead of a pinhole rebuilt from the player's eye. Rebuilding it ignores view
 * bobbing and FOV effects, which is why the bar used to drift away from the creature while the
 * player walked. 1.21.1 has no public "current projection" accessor, so the world pass captures
 * {@link RenderSystem#getProjectionMatrix()} once per frame and the HUD pass reuses it.</p>
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

    /** The world projection captured during the level pass, reused by the HUD pass. */
    private static final Matrix4f WORLD_PROJECTION = new Matrix4f();

    private static boolean warned;
    private static boolean reported;
    private static boolean hasProjection;

    private HealthBarOverlay() {
    }

    public static void register() {
        ClientEvents.addWorldPass((pose, buffers) -> captureProjection());
        ClientEvents.addHudPass(HealthBarOverlay::render);
    }

    private static void captureProjection() {
        WORLD_PROJECTION.set(RenderSystem.getProjectionMatrix());
        hasProjection = true;
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        RenderWatchdog.beat();
        ModSettings settings = ModSettings.get();
        HealthBarDisplay display = settings.healthBarDisplay();
        if (display == HealthBarDisplay.OFF || !hasProjection) {
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
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Matrix4f viewProjection = new Matrix4f(WORLD_PROJECTION)
                .mul(new Matrix4f().rotation(new Quaternionf(camera.rotation()).conjugate()));

        Font font = minecraft.font;
        float sizeFactor = settings.healthBarSize().factor();
        float worldWidth = WORLD_BAR_WIDTH * sizeFactor;
        // The captured matrix already carries the real FOV (sprint, underwater, effects), and its
        // [1][1] element is 1/tan(fov/2), so the size scaling needs no second source of truth.
        double tanHalfFov = 1.0 / Math.max(1.0E-4, WORLD_PROJECTION.m11());

        float[] screen = new float[2];
        for (LivingEntity entity : targets) {
            Vec3 anchor = anchorOf(entity, partialTick);
            double distance = eye.distanceTo(anchor);
            if (distance > RANGE || !project(viewProjection, eye, anchor, width, height, screen)) {
                continue;
            }

            double pixelsPerBlock = (height / 2.0) / (tanHalfFov * Math.max(0.25, distance));
            float barWidth = clamp((float) (worldWidth * pixelsPerBlock),
                    MIN_BAR_WIDTH, MAX_BAR_WIDTH);
            float barHeight = clamp(barWidth * BAR_ASPECT, MIN_BAR_HEIGHT, MAX_BAR_HEIGHT);
            float textScale = clamp((float) (worldWidth * TEXT_ASPECT * pixelsPerBlock
                    / Math.max(1, font.lineHeight)), MIN_TEXT_SCALE, MAX_TEXT_SCALE);

            float exactX = screen[0];
            float exactY = screen[1];
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

    private static Vec3 anchorOf(LivingEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + HEAD_GAP,
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    private static boolean project(Matrix4f viewProjection, Vec3 eye, Vec3 point,
                                   int width, int height, float[] out) {
        Vector4f clip = viewProjection.transform(new Vector4f(
                (float) (point.x - eye.x),
                (float) (point.y - eye.y),
                (float) (point.z - eye.z),
                1.0F));
        if (clip.w <= 1.0E-4F) {
            return false;
        }
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (Math.abs(ndcX) > 1.0F || Math.abs(ndcY) > 1.0F) {
            return false;
        }
        out[0] = (ndcX * 0.5F + 0.5F) * width;
        out[1] = (0.5F - ndcY * 0.5F) * height;
        return true;
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
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
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
            if (eye.distanceToSqr(anchorOf(living, partialTick)) > RANGE * RANGE) {
                continue;
            }
            found.add(living);
        }

        if (found.size() > MAX_BARS) {
            found.sort(Comparator.comparingDouble(
                    entity -> eye.distanceToSqr(anchorOf(entity, partialTick))));
            return new ArrayList<>(found.subList(0, MAX_BARS));
        }
        return found;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
