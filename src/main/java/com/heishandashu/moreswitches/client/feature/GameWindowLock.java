package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;

/**
 * Stops the game window from being resized. Position is left alone.
 *
 * <p>Two mechanisms are combined, because the first one alone is not enough:</p>
 * <ol>
 *   <li>{@code glfwSetWindowAttrib(GLFW_RESIZABLE, GLFW_FALSE)} removes the resize border from the
 *       native window. That border is what turns the cursor into a double arrow over an edge.</li>
 *   <li>The minimum and maximum sizes are pinned to the size at lock time, so nothing else can
 *       resize the window either: not an OS display change, and not a program dragging an edge that
 *       still hit-tests.</li>
 * </ol>
 *
 * <p>Deliberately <b>not</b> done: the position is never written back. The player can drag the
 * window anywhere and it stays there; only its size is frozen.</p>
 *
 * <p>Minecraft 1.21.1 creates its window through GLFW, so that is the API used here. The newer
 * SDL-based windowing does not exist on this version.</p>
 */
final class GameWindowLock {

    /** Vanilla's own minimum window size, restored when the switch goes off. */
    private static final int VANILLA_MIN_WIDTH = 320;
    private static final int VANILLA_MIN_HEIGHT = 240;

    /** GLFW's "no limit" value for window size limits. */
    private static final int NO_LIMIT = GLFW.GLFW_DONT_CARE;

    private boolean locked;

    private long windowHandle;

    private int lockedWidth;
    private int lockedHeight;

    /** True while the switch is on and the window's size is actually frozen. */
    boolean isLocked() {
        return this.locked;
    }

    /**
     * Stops the window from being resized at its current size.
     *
     * @return true when the size is frozen; false when the window cannot be locked right now, in
     *         which case the caller leaves the switch visually off instead of pretending
     */
    boolean apply(Window window) {
        if (this.locked) {
            return true;
        }
        long handle = window.getWindow();
        if (handle == 0L) {
            MoreSwitchesMod.LOGGER.warn(
                    "[{}] window lock: the window has no native handle yet -- not locking",
                    MoreSwitchesMod.MOD_ID);
            return false;
        }

        // The framebuffer size, divided by the GUI scale, is what the player perceives as the
        // window size; the window size itself is what GLFW limits apply to, so read that.
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetWindowSize(handle, width, height);
        this.lockedWidth = width[0];
        this.lockedHeight = height[0];

        if (this.lockedWidth <= 0 || this.lockedHeight <= 0) {
            MoreSwitchesMod.LOGGER.warn("[{}] window lock: read an invalid window size {}x{}",
                    MoreSwitchesMod.MOD_ID, this.lockedWidth, this.lockedHeight);
            return false;
        }

        this.windowHandle = handle;

        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowSizeLimits(handle, this.lockedWidth, this.lockedHeight,
                this.lockedWidth, this.lockedHeight);

        this.locked = true;
        MoreSwitchesMod.LOGGER.info(
                "[{}] window lock on: size fixed at {}x{}, position left free",
                MoreSwitchesMod.MOD_ID, this.lockedWidth, this.lockedHeight);
        return true;
    }

    /** Gives the window back its resize border. Position was never touched, so nothing to undo. */
    void release() {
        if (!this.locked) {
            return;
        }
        long handle = this.windowHandle;
        this.locked = false;
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwSetWindowSizeLimits(handle, VANILLA_MIN_WIDTH, VANILLA_MIN_HEIGHT,
                NO_LIMIT, NO_LIMIT);
        MoreSwitchesMod.LOGGER.info("[{}] window lock off", MoreSwitchesMod.MOD_ID);
    }
}
