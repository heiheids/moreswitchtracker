package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.client.Minecraft;

/**
 * Applies and releases the window size lock whenever the switch flips.
 *
 * <p>All SDL work lives in {@link GameWindowLock}; this class only translates "the switch changed"
 * into "freeze the size" or "give the border back", and remembers whether the lock is currently
 * installed so the two calls cannot drift apart.</p>
 *
 * <p>There is no per-tick work: the position is not managed at all, and the size is pinned through
 * the window's own minimum and maximum size, so nothing has to be corrected every frame.</p>
 */
public final class WindowLockModule implements ClientFeature {

    private final GameWindowLock windowLock = new GameWindowLock();

    private boolean applied;

    @Override
    public void register(String switchId) {
        // Apply immediately if the file says the switch was left on, so the setting survives a
        // restart exactly like the player left it.
        onEnabledChanged(enabled(switchId));
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (enabled == (this.applied && this.windowLock.isLocked())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) {
            MoreSwitchesMod.LOGGER.warn("[{}] window lock: no window yet, the switch has no effect",
                    MoreSwitchesMod.MOD_ID);
            return;
        }

        if (enabled) {
            this.applied = this.windowLock.apply(minecraft.getWindow());
        } else {
            this.windowLock.release();
            this.applied = false;
        }

        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {} (applied: {})",
                MoreSwitchesMod.MOD_ID, SwitchIds.WINDOW_LOCK, enabled, this.applied);
    }
}
