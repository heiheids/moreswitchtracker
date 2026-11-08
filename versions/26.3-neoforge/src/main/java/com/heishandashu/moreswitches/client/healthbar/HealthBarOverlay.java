package com.heishandashu.moreswitches.client.healthbar;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.HealthBarCategory;
import com.heishandashu.moreswitches.config.HealthBarDisplay;
import com.heishandashu.moreswitches.config.HealthBarSize;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import com.heishandashu.tracker.client.render.ScreenProjection;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 血条 / overhead health bar: a slim red bar and the current/maximum numbers above a creature.
 *
 * <p>Drawn on the HUD rather than in the world. The tracker half already owns a screen projection
 * helper, so an entity's head is projected once per frame and the bar is then laid out in GUI
 * pixels: that keeps it pixel-crisp at any GUI scale, keeps it the same size at any distance, and
 * keeps the 大/中/小 setting exact. The cost is that terrain does not hide a bar, which matches how
 * the rest of this mod's overlays behave.</p>
 *
 * <p>The whole pass is skipped while the page's display mode is {@code off}, so an unused feature
 * costs one field read per frame.</p>
 */
public final class HealthBarOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(MoreSwitchesMod.MOD_ID, "health_bar");

    /** Base geometry at 中 size, in GUI pixels; the size setting multiplies these. */
    private static final int BASE_BAR_WIDTH = 58;
    private static final int BASE_BAR_HEIGHT = 6;

    /** Bars further away than this are not drawn. */
    private static final double RANGE = 48.0;

    /** Upper bound on bars per frame, so a crowded raid cannot turn into a wall of red. */
    private static final int MAX_BARS = 48;

    /** How far above the entity's bounding box the bar sits, in blocks. */
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
        TrackerClientEvents.onHudLayer(HUD_ID, HealthBarOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
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
            draw(graphics, minecraft, level, self, settings, display);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                MoreSwitchesMod.LOGGER.warn("[{}] health bar overlay could not draw",
                        MoreSwitchesMod.MOD_ID, t);
            }
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, ClientLevel level,
                             LocalPlayer self, ModSettings settings, HealthBarDisplay display) {
        List<LivingEntity> targets = collect(level, self, settings);
        if (!reported && level != null) {
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
        Vec3 eye = self.getEyePosition();
        Vec3 forward = self.getViewVector(1.0f);
        double fov = minecraft.options.fov().get();
        Font font = minecraft.font;

        float factor = settings.healthBarSize().factor();
        int barWidth = Math.max(18, Math.round(BASE_BAR_WIDTH * factor));
        int barHeight = Math.max(3, Math.round(BASE_BAR_HEIGHT * factor));
        int textHeight = Math.max(5, Math.round(font.lineHeight * factor));

        for (LivingEntity entity : targets) {
            double anchorY = entity.getBoundingBox().maxY + HEAD_GAP;
            Vec3 anchor = new Vec3(entity.getX(), anchorY, entity.getZ());
            ScreenProjection.Projected projected =
                    ScreenProjection.project(eye, forward, fov, width, height, anchor, 0.0);
            if (!projected.onScreen() || projected.distance() > RANGE) {
                continue;
            }

            int centerX = (int) Math.round(projected.screenX());
            int centerY = (int) Math.round(projected.screenY());

            if (display.showsBar()) {
                drawBar(graphics, entity, centerX, centerY, barWidth, barHeight);
            }
            if (display.showsNumbers()) {
                int textTop = display.showsBar()
                        ? centerY - barHeight / 2 - textHeight - 1
                        : centerY - textHeight / 2;
                drawNumbers(graphics, font, entity, centerX, textTop, factor);
            }
        }
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

        // Dark outline, then the empty track, then the red fill with a one-pixel gloss line.
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

    /** Whole numbers read better than decimals; half hearts keep one decimal. */
    private static String formatHealth(float value) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05F) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Living creatures in range whose category is switched on, nearest first. */
    private static List<LivingEntity> collect(ClientLevel level, LocalPlayer self,
                                              ModSettings settings) {
        Vec3 eye = self.getEyePosition();
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
            if (eye.distanceToSqr(living.getX(), living.getBoundingBox().maxY, living.getZ())
                    > RANGE * RANGE) {
                continue;
            }
            found.add(living);
        }

        if (found.size() > MAX_BARS) {
            found.sort(Comparator.comparingDouble(
                    entity -> eye.distanceToSqr(entity.getX(), entity.getY(), entity.getZ())));
            return new ArrayList<>(found.subList(0, MAX_BARS));
        }
        return found;
    }
}
