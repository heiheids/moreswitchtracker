package com.heishandashu.moreswitches;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared identity of the 26.3 build.
 *
 * <p>There is no second loader entrypoint here: {@code com.heishandashu.tracker.TrackerMod} is the
 * Fabric {@code ModInitializer}, and it initialises both halves. This class only owns the id,
 * log channel and identifier helper.</p>
 */
public final class MoreSwitchesMod {

    public static final String MOD_ID = "moreswitchtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("MoreSwitchTracker");

    private MoreSwitchesMod() {
    }

    /** {@code moreswitchtracker:<path>} */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
