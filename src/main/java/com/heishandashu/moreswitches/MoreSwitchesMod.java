package com.heishandashu.moreswitches;

import com.heishandashu.tracker.TrackerMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint of the 1.21.1 NeoForge build.
 *
 * <p>The mod is one small settings panel: a J-key screen with a utility-toggles page and a tracking
 * page. Each page keeps its own registry and settings file; this class only starts both.</p>
 *
 * <p>Author: 黑山大叔 — https://space.bilibili.com/19428259<br>
 * GitHub: https://github.com/heiheids</p>
 */
@Mod(MoreSwitchesMod.MOD_ID)
public final class MoreSwitchesMod {

    /** Registry namespace; also the prefix of every translation key this mod owns. */
    public static final String MOD_ID = "moreswitchtracker";

    public static final Logger LOGGER = LoggerFactory.getLogger("MoreSwitchTracker");

    /**
     * @param modBus the mod's own event bus, used by both pages' initialisers
     */
    public MoreSwitchesMod(IEventBus modBus) {
        TrackerMod.initialize(modBus);
        LOGGER.info("[{}] common init (settings: {}, tracker: {})", MOD_ID,
                FMLPaths.CONFIGDIR.get().resolve("moreswitches.json"),
                FMLPaths.CONFIGDIR.get().resolve("tracker.json"));
    }

    /** {@code moreswitchtracker:<path>} — the only place identifiers are built. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
