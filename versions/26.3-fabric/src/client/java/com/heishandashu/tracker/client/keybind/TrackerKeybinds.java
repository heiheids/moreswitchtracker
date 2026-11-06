package com.heishandashu.tracker.client.keybind;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.ui.TrackerSettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Key bindings for the mod.
 *
 * <p>26.3 renamed the Fabric helper ({@code KeyBindingHelper} -&gt; {@code KeyMappingHelper})
 * and turned a key category into a record that wraps an {@link Identifier}, so the category
 * label is resolved from {@code key.category.<namespace>.<path>}.</p>
 */
public final class TrackerKeybinds {

    /** Own category so the binding is easy to find in the vanilla key-bindings screen. */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "main"));

    /** Default: K. Players can rebind it in Options -> Controls -> Key Binds. */
    public static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key." + TrackerMod.MOD_ID + ".open_settings", InputConstants.KEY_K, CATEGORY));

    private TrackerKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                Screen current = client.gui.screen();
                // Do not stack the settings screen on top of itself.
                if (!(current instanceof TrackerSettingsScreen)) {
                    client.gui.setScreen(new TrackerSettingsScreen(current));
                }
            }
        });
    }
}
