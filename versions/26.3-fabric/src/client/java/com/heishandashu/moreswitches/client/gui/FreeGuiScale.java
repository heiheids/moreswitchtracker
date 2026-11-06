package com.heishandashu.moreswitches.client.gui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/**
 * Holds a free (non-integer) GUI scale.
 *
 * <p>Minecraft normally snaps the interface to a whole multiple of its pixel grid: the scale is an
 * {@code int}, so the sizes a player can pick are 1x, 2x, 3x, 4x and so on. That leaves a real gap
 * on small or high-DPI screens, where 2x is too small and 3x is too large.</p>
 *
 * <p>{@code requested} is what the player asked for and is what gets saved; the applied value is
 * that request capped so the interface can never become too small to use. Keeping the two apart
 * means a scale chosen on a large window survives a session on a small one instead of being
 * silently rewritten.</p>
 *
 * <p>A request of {@code 0} (the default) means "feature off": vanilla's own integer scale applies
 * and nothing in the render path is touched.</p>
 */
public final class FreeGuiScale {

    /** Below 0.5 the interface would be larger than the window. */
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 8.0F;

    /** Smallest step the slider offers, so 2.05 and 2.75 are both reachable. */
    public static final float STEP = 0.05F;

    /**
     * Smallest interface the settings screen can still be driven at.
     *
     * <p>The free scale may go past vanilla's own limit, but not so far that the screen holding the
     * control to undo it would stop working.</p>
     */
    public static final int MIN_GUI_WIDTH = 320;
    public static final int MIN_GUI_HEIGHT = 180;

    /** 0 means "feature off, use the vanilla integer scale". */
    private static volatile float requested;

    private FreeGuiScale() {
    }

    /** True when the player has chosen a free scale; false means vanilla drives the size. */
    public static boolean active() {
        return requested > 0.0F;
    }

    /** The value the player asked for, without the window limit. */
    public static float requested() {
        return requested;
    }

    /**
     * The scale to render with: the request, capped so the interface stays usable.
     *
     * <p>Every patch in the render path calls this rather than {@link #requested()}, so the
     * projection, the scissor rectangles and the window's own scaled size can never disagree.</p>
     */
    public static float scale() {
        Window window = window();
        if (window == null) {
            return requested;
        }
        return appliedFor(window.getWidth(), window.getHeight());
    }

    /**
     * The same cap as {@link #scale()}, for callers that already know the framebuffer size.
     *
     * <p>Mixins use this: they read the size from their own target's fields, which works even
     * before {@code Minecraft.getInstance().getWindow()} is usable.</p>
     */
    public static float appliedFor(int framebufferWidth, int framebufferHeight) {
        if (requested <= 0.0F) {
            return 0.0F;
        }
        return Math.min(requested, maxFor(framebufferWidth, framebufferHeight));
    }

    /** Largest free scale that still leaves a usable interface for this framebuffer. */
    public static float maxUsableScale() {
        Window window = window();
        if (window == null) {
            return MAX_SCALE;
        }
        return maxFor(window.getWidth(), window.getHeight());
    }

    public static void setScale(float value) {
        requested = clamp(value);
    }

    public static void disable() {
        requested = 0.0F;
    }

    /** Rounds to {@link #STEP} so the slider and the typed value agree on what is reachable. */
    public static float snap(float value) {
        float stepped = Math.round(value / STEP) * STEP;
        return clamp((float) (Math.round(stepped * 100.0F) / 100.0F));
    }

    private static float maxFor(int framebufferWidth, int framebufferHeight) {
        float byWidth = framebufferWidth / (float) MIN_GUI_WIDTH;
        float byHeight = framebufferHeight / (float) MIN_GUI_HEIGHT;
        float limit = Math.min(byWidth, byHeight);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, limit));
    }

    private static float clamp(float value) {
        if (value < MIN_SCALE) {
            return MIN_SCALE;
        }
        return Math.min(value, MAX_SCALE);
    }

    private static Window window() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getWindow();
    }
}
