package com.heishandashu.moreswitches.quiet;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * The three "stop bothering me" switches.
 *
 * <p>They share a class because they are all the same shape: one boolean in the settings file
 * decides whether vanilla is allowed to do something intrusive. Keeping them together avoids
 * three files that would each hold a single line, while the three named accessors keep the call
 * sites readable.</p>
 */
public final class QuietRules {

    private QuietRules() {
    }

    /**
     * Whether an incompatible resource pack may be enabled without the confirmation dialog.
     *
     * <p>The dialog exists to warn that a pack was built for another Minecraft version. A player
     * who turned this on has read that warning once and does not want it again on every change.</p>
     */
    public static boolean skipResourcePackWarning() {
        return ModSettings.get().isEnabled(SwitchIds.RESOURCE_PACK_WARNING_OFF);
    }

    /** Whether campfires should stop producing smoke. */
    public static boolean suppressCampfireSmoke() {
        return ModSettings.get().isEnabled(SwitchIds.CAMPFIRE_SMOKE_OFF);
    }

    /**
     * Whether the tutorial should be prevented from starting.
     *
     * <p>Vanilla's tutorial drives every "press W to walk" style toast. Stopping it at the source
     * means no tutorial message of any kind can appear, rather than hiding each one separately.</p>
     */
    public static boolean suppressTutorial() {
        return ModSettings.get().isEnabled(SwitchIds.TUTORIAL_OFF);
    }
}
