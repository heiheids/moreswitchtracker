package com.heishandashu.moreswitches.client.keybind;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * The single J binding of the mod.
 *
 * <p>Tracker's old K binding is intentionally not registered: both former screens now live behind
 * this one tabbed screen. 26.3 categories are records wrapping an {@link Identifier}.</p>
 */
public final class SettingsKeybinds {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(
                    MoreSwitchesMod.MOD_ID, "main"));

    public static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key." + MoreSwitchesMod.MOD_ID + ".open_settings",
                    InputConstants.KEY_J, CATEGORY));

    private SettingsKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                Screen current = client.gui.screen();
                if (current instanceof SettingsScreen) {
                    client.gui.setScreen(null);
                } else if (current == null) {
                    client.gui.setScreen(new SettingsScreen(null));
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
