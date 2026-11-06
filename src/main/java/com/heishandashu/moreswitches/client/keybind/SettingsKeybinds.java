package com.heishandashu.moreswitches.client.keybind;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ClientTickHook;
import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * The one key binding the mod owns.
 *
 * <p>Default is <b>J</b>. Tracker's old K binding is intentionally gone: two keys opening two
 * different settings screens would contradict the tabbed interface. J is unbound in vanilla,
 * so the chat key T is not taken away.</p>
 */
public final class SettingsKeybinds {

    public static final String CATEGORY = "key.category." + MoreSwitchesMod.MOD_ID + ".main";

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key." + MoreSwitchesMod.MOD_ID + ".open_settings",
            InputConstants.KEY_J,
            CATEGORY);

    private SettingsKeybinds() {
    }

    /** Called once from the client initialiser; the key needs no other setup on this platform. */
    public static void register() {
        ClientTickHook.onEndTick(SettingsKeybinds::onEndTick);
        MoreSwitchesMod.LOGGER.info("[{}] settings key bound by default to J", MoreSwitchesMod.MOD_ID);
    }

    private static void onEndTick(Minecraft minecraft) {
        while (OPEN_SETTINGS.consumeClick()) {
            Screen current = minecraft.screen;
            if (current instanceof SettingsScreen) {
                // The key toggles: pressing it again closes the screen it opened.
                minecraft.setScreen(null);
            } else if (current == null) {
                minecraft.setScreen(new SettingsScreen(null));
            }
            // Any other screen (chat, inventory, pause) keeps the key to itself so this binding can
            // never steal a key press from a screen the player is typing in.
        }
    }
}
