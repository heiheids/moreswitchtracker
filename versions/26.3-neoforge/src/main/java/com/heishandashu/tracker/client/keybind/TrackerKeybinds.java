package com.heishandashu.tracker.client.keybind;

import com.heishandashu.tracker.TrackerMod;
import com.heishandashu.tracker.client.compat.TrackerClientEvents;
import com.heishandashu.tracker.client.ui.TrackerSettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Key bindings for the mod.
 *
 * <p>26.3 turned a key category into a record that wraps an {@link Identifier}, so the category
 * label is resolved from {@code key.category.<namespace>.<path>}.</p>
 *
 * <p>Fabric's {@code KeyMappingHelper.registerKeyMapping} both created the binding and registered
 * it. NeoForge separates the two: the {@link KeyMapping} object is built here, and the binding is
 * handed to the mod bus in {@link #registerKeyMappings(RegisterKeyMappingsEvent)}. The custom
 * category is declared in that same event so it sorts next to the vanilla ones instead of
 * silently sharing the "Misc" group.</p>
 */
public final class TrackerKeybinds {

    /** Own category so the binding is easy to find in the vanilla key-bindings screen. */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(TrackerMod.MOD_ID, "main"));

    /** Default: K. Players can rebind it in Options -> Controls -> Key Binds. */
    public static final KeyMapping OPEN_SETTINGS =
            new KeyMapping("key." + TrackerMod.MOD_ID + ".open_settings", InputConstants.KEY_K, CATEGORY);

    private TrackerKeybinds() {
    }

    /** Mod bus: the binding and its category only exist once the game asks for them. */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_SETTINGS);
    }

    /** Opens the settings screen while the binding is being pressed. */
    public static void register() {
        TrackerClientEvents.onClientTick(() -> {
            Minecraft client = Minecraft.getInstance();
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
