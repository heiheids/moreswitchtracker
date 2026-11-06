package com.heishandashu.tracker.compat;

import com.heishandashu.moreswitches.client.ui.SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu hook: adds the "config" button that opens our settings screen from the mods list.
 *
 * <p>Mod Menu is compile-only: when it is not installed the {@code modmenu} entrypoint is
 * simply never loaded, so this class never resolves.</p>
 *
 * <p>The return type is spelled {@code ConfigScreenFactory<Screen>} instead of the wildcard
 * so the lambda has a concrete target type.</p>
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> new SettingsScreen(parent);
    }
}
