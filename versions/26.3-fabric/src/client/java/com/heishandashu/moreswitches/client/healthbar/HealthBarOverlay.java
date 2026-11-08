package com.heishandashu.moreswitches.client.healthbar;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.HealthBarCategory;
import com.heishandashu.moreswitches.config.HealthBarDisplay;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.tracker.client.diagnostic.RenderWatchdog;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 血条 / overhead health bar: a slim red bar and the current/maximum numbers above a creature.
 *
 * <p>Drawn on the HUD rather than in the world, so the bar stays crisp at any GUI scale. What makes
 * it sit on the creature exactly:</p>
 *
 * <ul>
 *   <li><b>The camera's own matrix.</b> The world was rendered with {@link Camera}, which includes
 *       view bobbing, FOV effects and a possible third-person offset. Rebuilding a pinhole from the
 *       player's eye and {@code options.fov()} instead makes the bar drift away from the creature
 *       as soon as the player walks — that is what used to happen. Projecting with
 *       {@code getViewRotationProjectionMatrix} cannot disagree with the frame that was drawn.</li>
 *   <li><b>Perspective size.</b> The bar has a size in blocks and is converted to pixels with the
 *       camera's real FOV, so a creature twice as far away gets a bar half as wide.</li>
 *   <li><b>Interpolation.</b> Entity positions only move 20 times a second; they are lerped with the
 *       frame's partial tick and placed on sub-pixel offsets so the bar does not stutter.</li>
 * </ul>
 */
public final class HealthBarOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(MoreSwitchesMod.MOD_ID, "health_bar");

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
        HudElementRegistry.addLast(HUD_ID, HealthBarOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        RenderWatchdog.beat();
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
            draw(graphics, minecraft, level, self, settings, display,
                    delta.getGameTimeDeltaPartialTick(false));
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                MoreSwitchesMod.LOGGER.warn("[{}] health bar overlay could not draw",
                        MoreSwitchesMod.MOD_ID, t);
            }
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, ClientLevel level,
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
        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
        Font font = minecraft.font;
        float sizeFactor = settings.healthBarSize().factor();
        float worldWidth = WORLD_BAR_WIDTH * sizeFactor;
        double tanHalfFov = Math.tan(Math.toRadians(camera.getFov()) / 2.0);

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

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(exactX - baseX, exactY - baseY);

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

            pose.popMatrix();
        }
    }

    /** World point above the creature, interpolated to this frame. */
    private static Vec3 anchorOf(LivingEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() + HEAD_GAP,
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    /**
     * Projects a world point with the matrix the frame was drawn with.
     *
     * @return false when the point is behind the camera or outside the view
     */
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

    private static void drawBar(GuiGraphicsExtractor graphics, LivingEntity entity,
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

    private static void drawNumbers(GuiGraphicsExtractor graphics, Font font, LivingEntity entity,
                                    int centerX, int textTop, float scale) {
        String label = formatHealth(entity.getHealth()) + "/" + formatHealth(entity.getMaxHealth());
        float scaledWidth = font.width(label) * scale;
        float x = centerX - scaledWidth / 2.0F;

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, textTop);
        pose.scale(scale, scale);
        graphics.text(font, label, 0, 0, TEXT_COLOR, true);
        pose.popMatrix();
    }

    private static String formatHealth(float value) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05F) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Living creatures in range whose category is switched on, nearest first. */
    private static List<LivingEntity> collect(ClientLevel level, LocalPlayer self,
                                              ModSettings settings, float partialTick) {
        Vec3 eye = Minecraft.getInstance().gameRenderer.mainCamera().position();
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
