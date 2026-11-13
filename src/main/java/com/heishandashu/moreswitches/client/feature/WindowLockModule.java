package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ClientTickHook;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.client.Minecraft;

/**
 * Applies and releases the window size lock whenever the switch flips.
 *
 * <p>All window work lives in {@link GameWindowLock}; this class only translates "the switch
 * changed" into "freeze the size" or "give the border back".</p>
 *
 * <p>The lock is applied from the client tick loop rather than only at the moment the switch is
 * read, and it re-applies itself if the native state drifts. Two reasons:</p>
 * <ul>
 *   <li>The client entry point runs before the renderer exists, and the renderer's start-up writes
 *       its own window size limits, quietly undoing an earlier pin.</li>
 *   <li>A window can be recreated (display change, fullscreen transition), which also drops both
 *       the border flag and the limits.</li>
 * </ul>
 *
 * <p>The check is cheap -- it only reads back two pieces of native state -- and it is skipped
 * entirely while the switch is off.</p>
 */
public final class WindowLockModule implements ClientFeature {

    private final GameWindowLock windowLock = new GameWindowLock();

    private boolean applied;

    private boolean tickHooked;

    @Override
    public void register(String switchId) {
        // Try immediately so a restart looks right from the first frame, then keep the lock alive.
        onEnabledChanged(enabled(switchId));
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        if (enabled) {
            if (this.windowLock.isLocked() && this.windowLock.isStillApplied()) {
                return;
            }
            if (minecraft.getWindow() == null) {
                MoreSwitchesMod.LOGGER.warn(
                        "[{}] window lock: no window yet, the switch will apply on the first tick",
                        MoreSwitchesMod.MOD_ID);
                ensureTickHook();
                return;
            }
            this.applied = this.windowLock.apply(minecraft.getWindow());
            ensureTickHook();
        } else {
            this.windowLock.release();
            this.applied = false;
        }

        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {} (applied: {})",
                MoreSwitchesMod.MOD_ID, SwitchIds.WINDOW_LOCK, enabled, this.applied);
    }

    /**
     * Keeps the lock in place.
     *
     * <p>Registered once. Every tick it asks the window two questions: is the lock still what the
     * player asked for, and does the window exist yet. Both answers are cheap native reads; while
     * the switch is off they are not asked at all.</p>
     */
    private void ensureTickHook() {
        if (this.tickHooked) {
            return;
        }
        this.tickHooked = true;
        ClientTickHook.onEndTick(client -> {
            if (!enabled(SwitchIds.WINDOW_LOCK)) {
                return;
            }
            if (client.getWindow() == null) {
                return;
            }
            if (!this.windowLock.isLocked()) {
                this.applied = this.windowLock.apply(client.getWindow());
                return;
            }
            if (!this.windowLock.isStillApplied()) {
                this.windowLock.reapply();
                MoreSwitchesMod.LOGGER.debug(
                        "[{}] window lock: the game had reset the window state -- re-applied",
                        MoreSwitchesMod.MOD_ID);
            }
        });
    }
}
