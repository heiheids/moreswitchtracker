package com.heishandashu.moreswitches.client.i18n;

import com.heishandashu.moreswitches.i18n.ModLang;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Reads the mod's two language files whenever the client reloads its resources.
 *
 * <p>Doing it in a reload listener (instead of lazily on first screen open) means the text is
 * already there when the screen is drawn, and a resource pack that ships a translation is picked up
 * on {@code F3+T} like any other resource.</p>
 *
 * <p>NeoForge registers this through {@code RegisterClientReloadListenersEvent}; the listener
 * interface itself is plain vanilla, so this class holds no platform types.</p>
 */
public final class ClientLanguageReload implements ResourceManagerReloadListener {

    private ClientLanguageReload() {
    }

    /** The listener to hand to NeoForge's registration event. */
    public static ResourceManagerReloadListener listener() {
        return new ClientLanguageReload();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        ModLang.reload(manager, Minecraft.getInstance().getLanguageManager().getSelected());
    }
}
