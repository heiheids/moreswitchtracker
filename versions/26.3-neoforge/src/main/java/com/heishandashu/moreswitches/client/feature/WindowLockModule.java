package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ClientTickHook;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.client.Minecraft;

/**
 * Applies and releases the window size lock whenever the switch flips.
 *
 * <p>All native window work lives in {@link GameWindowLock}; this class only translates "the switch
 * changed" into "freeze the size" or "give the border back".</p>
 *
 * <p><b>Why the lock is applied from the tick loop as well:</b> the client entry point runs before
 * the window and the renderer are fully set up, and both write window state of their own while they
 * finish. A pin written there is silently replaced, which is why the switch used to come back
 * unlocked after a restart until the player flipped it twice. The tick handler re-asserts the lock
 * whenever the native state no longer matches, and does nothing at all while the switch is off.</p>
 */
public final class WindowLockModule implements ClientFeature {

    private final GameWindowLock windowLock = new GameWindowLock();

    private boolean applied;

    private boolean tickHooked;

    @Override
    public void register(String switchId) {
        // Apply immediately if the file says the switch was left on, so the setting survives a
        // restart exactly like the player left it.
        onEnabledChanged(enabled(switchId));
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (enabled && this.applied && this.windowLock.isLocked()
                && this.windowLock.isStillApplied()) {
            return;
        }
        if (!enabled && !this.applied && !this.windowLock.isLocked()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) {
            MoreSwitchesMod.LOGGER.warn(
                    "[{}] window lock: no window yet, the switch will apply on the first tick",
                    MoreSwitchesMod.MOD_ID);
            ensureTickHook();
            return;
        }

        if (enabled) {
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
     * <p>Registered once. Each tick it does two cheap native reads: does the window exist yet, and
     * does its state still match the lock. The first matters because the entry point runs before
     * the window is usable; the second because the game overwrites the pin during start-up and on
     * window rebuilds. Nothing is done while the switch is off.</p>
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
