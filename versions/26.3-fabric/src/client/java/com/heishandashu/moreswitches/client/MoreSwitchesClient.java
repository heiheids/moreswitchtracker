package com.heishandashu.moreswitches.client;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.feature.ClientFeatures;
import com.heishandashu.moreswitches.client.gui.FreeGuiScale;
import com.heishandashu.moreswitches.client.gui.GuiScaleApplier;
import com.heishandashu.moreswitches.client.healthbar.HealthBarOverlay;
import com.heishandashu.tracker.client.diagnostic.RenderWatchdog;
import com.heishandashu.moreswitches.client.keybind.SettingsKeybinds;
import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client bootstrap for the switches page.
 *
 * <p>Called from the client initializer. It starts the feature modules, registers the single J
 * binding, and optionally opens the settings screen for verification runs.</p>
 */
public final class MoreSwitchesClient {

    private static final int DEBUG_OPEN_DELAY_TICKS = 80;
    private static final String DEBUG_OPEN_PROPERTY = "moreswitchtracker.debugOpenSettings";

    /** Prototype hook for verifying a fractional GUI scale; 0 or absent keeps vanilla behaviour. */
    private static final String DEBUG_GUI_SCALE_PROPERTY = "moreswitchtracker.debugGuiScale";

    private MoreSwitchesClient() {
    }

    public static void initializeClient() {
        ClientFeatures.registerAll();
        SettingsKeybinds.register();
        HealthBarOverlay.register();
        RenderWatchdog.start();
        // One tick hook for every scale change the 自由界面 page records.
        ClientTickHook.onEndTick(client -> GuiScaleApplier.applyIfPending());
        MoreSwitchesMod.LOGGER.info("[{}] client ready -- {} More Switches modules",
                MoreSwitchesMod.MOD_ID, SwitchIds.all().size());

        String debugScale = System.getProperty(DEBUG_GUI_SCALE_PROPERTY);
        if (debugScale != null && !debugScale.isBlank()) {
            FreeGuiScale.setScale(Float.parseFloat(debugScale.trim()));
            // The window already exists by now, so re-run the resize once a tick has passed and the
            // render targets are up; that is what applies the new scale to the live window.
            ClientTickHook.onEndTick(new java.util.function.Consumer<>() {
                private int ticks;

                @Override
                public void accept(Minecraft client) {
                    if (this.ticks++ != 20) {
                        return;
                    }
                    client.resizeGui();
                    MoreSwitchesMod.LOGGER.info("[{}] debug: free GUI scale = {} (scaled {}x{})",
                            MoreSwitchesMod.MOD_ID, FreeGuiScale.scale(),
                            client.getWindow().getGuiScaledWidth(),
                            client.getWindow().getGuiScaledHeight());
                }
            });
        }

        if (Boolean.getBoolean(DEBUG_OPEN_PROPERTY)) {
            ClientTickHook.onEndTick(new java.util.function.Consumer<>() {
                private int ticks;

                @Override
                public void accept(Minecraft client) {
                    if (this.ticks++ < DEBUG_OPEN_DELAY_TICKS) {
                        return;
                    }
                    Screen current = client.gui.screen();
                    if (current instanceof SettingsScreen) {
                        return;
                    }
                    MoreSwitchesMod.LOGGER.info("[{}] debug: opening the settings screen (was: {})",
                            MoreSwitchesMod.MOD_ID,
                            current == null ? "null" : current.getClass().getName());
                    client.gui.setScreen(new SettingsScreen(current));
                }
            });
        }

        restoreSavedGuiScale();
    }

    /**
     * Re-applies the free scale saved by the 自由界面 page.
     *
     * <p>The value has to be in place before the window settles, and one tick is the earliest point
     * where the window is certainly usable. Without this the setting would only come back the next
     * time the player opened the page.</p>
     */
    private static void restoreSavedGuiScale() {
        float saved = ModSettings.get().guiScale();
        if (saved <= 0.0F) {
            return;
        }
        FreeGuiScale.setScale(saved);
        ClientTickHook.onEndTick(new java.util.function.Consumer<>() {
            private int ticks;

            @Override
            public void accept(Minecraft client) {
                if (this.ticks++ != 1) {
                    return;
                }
                GuiScaleApplier.requestApply();
                MoreSwitchesMod.LOGGER.info("[{}] free GUI scale restored: {}x",
                        MoreSwitchesMod.MOD_ID, FreeGuiScale.scale());
            }
        });
    }
}
