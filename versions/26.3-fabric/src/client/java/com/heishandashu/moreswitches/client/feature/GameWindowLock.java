package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.BufferUtils;
import org.lwjgl.sdl.SDLVideo;

import java.nio.IntBuffer;

/**
 * Stops the game window from being resized. Position is left alone.
 *
 * <p>26.3 moved the native window from GLFW to SDL3. The equivalent calls are
 * {@link SDLVideo#SDL_SetWindowResizable}, {@link SDLVideo#SDL_SetWindowMinimumSize} and
 * {@link SDLVideo#SDL_SetWindowMaximumSize}; the native handle comes from
 * {@link Window#handle()}.</p>
 */
final class GameWindowLock {

    private static final int VANILLA_MIN_WIDTH = 320;
    private static final int VANILLA_MIN_HEIGHT = 240;

    /** SDL: 0 means "no limit". */
    private static final int NO_LIMIT = 0;

    private boolean locked;
    private long windowHandle;
    private int lockedWidth;
    private int lockedHeight;

    boolean isLocked() {
        return this.locked;
    }

    boolean apply(Window window) {
        if (this.locked) {
            return true;
        }
        long handle = window.handle();
        if (handle == 0L) {
            MoreSwitchesMod.LOGGER.warn(
                    "[{}] window lock: the window has no native handle yet -- not locking",
                    MoreSwitchesMod.MOD_ID);
            return false;
        }

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        SDLVideo.SDL_GetWindowSize(handle, width, height);
        this.lockedWidth = width.get(0);
        this.lockedHeight = height.get(0);

        if (this.lockedWidth <= 0 || this.lockedHeight <= 0) {
            MoreSwitchesMod.LOGGER.warn("[{}] window lock: read an invalid window size {}x{}",
                    MoreSwitchesMod.MOD_ID, this.lockedWidth, this.lockedHeight);
            return false;
        }

        this.windowHandle = handle;
        SDLVideo.SDL_SetWindowResizable(handle, false);
        SDLVideo.SDL_SetWindowMinimumSize(handle, this.lockedWidth, this.lockedHeight);
        SDLVideo.SDL_SetWindowMaximumSize(handle, this.lockedWidth, this.lockedHeight);

        this.locked = true;
        MoreSwitchesMod.LOGGER.info(
                "[{}] window lock on: size fixed at {}x{}, position left free",
                MoreSwitchesMod.MOD_ID, this.lockedWidth, this.lockedHeight);
        return true;
    }

    void release() {
        if (!this.locked) {
            return;
        }
        long handle = this.windowHandle;
        this.locked = false;
        SDLVideo.SDL_SetWindowResizable(handle, true);
        SDLVideo.SDL_SetWindowMinimumSize(handle, VANILLA_MIN_WIDTH, VANILLA_MIN_HEIGHT);
        SDLVideo.SDL_SetWindowMaximumSize(handle, NO_LIMIT, NO_LIMIT);
        MoreSwitchesMod.LOGGER.info("[{}] window lock off", MoreSwitchesMod.MOD_ID);
    }
}
