package com.heishandashu.moreswitches.client.keybind;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * The single J binding of the mod.
 *
 * <p>Tracker's old K binding is intentionally not registered: both former screens now live behind
 * this one tabbed screen. 26.3 categories are records wrapping an {@link Identifier}; NeoForge
 * receives the mapping through {@link RegisterKeyMappingsEvent}.</p>
 */
public final class SettingsKeybinds {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(
                    MoreSwitchesMod.MOD_ID, "main"));

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key." + MoreSwitchesMod.MOD_ID + ".open_settings",
            InputConstants.KEY_J, CATEGORY);

    private static boolean tickRegistered;

    private SettingsKeybinds() {
    }

    /** Called on the mod bus during client registration. */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    /** Called after the client buses are wired; only installs the once-per-tick click handler. */
    public static void register() {
        if (tickRegistered) {
            return;
        }
        tickRegistered = true;
        TrackerClientEvents.onClientTick(() -> {
            while (OPEN_SETTINGS.consumeClick()) {
                Screen current = net.minecraft.client.Minecraft.getInstance().gui.screen();
                if (current instanceof SettingsScreen) {
                    net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);
                } else if (current == null) {
                    net.minecraft.client.Minecraft.getInstance().gui.setScreen(
                            new SettingsScreen(null));
                }
            }
        });
        MoreSwitchesMod.LOGGER.info("[{}] settings key bound by default to J",
                MoreSwitchesMod.MOD_ID);
    }

    /** Current bound key, or "?" when the player cleared it. */
    public static String keyLabel() {
        return OPEN_SETTINGS.isUnbound()
                ? "?"
                : OPEN_SETTINGS.getTranslatedKeyMessage().getString();
    }
}
