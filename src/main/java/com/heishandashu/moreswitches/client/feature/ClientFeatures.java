package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchDefinition;
import com.heishandashu.moreswitches.config.SwitchIds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The client's set of switchable behaviours.
 *
 * <p>This is the only place that maps a switch id to the module implementing it. The settings
 * screen asks this class "is it on" and "turn it on"; the mixin asks it "may this use go
 * through?". Neither of them knows which class implements which feature.</p>
 *
 * <p>Modules are created once, in screen order, so a switch flip reaches every listener exactly
 * once and the on/off state cannot diverge from the settings file.</p>
 */
public final class ClientFeatures {

    private static final Map<String, ClientFeature> MODULES = new LinkedHashMap<>();

    private static FireworkCooldownModule fireworkCooldown;

    private ClientFeatures() {
    }

    /** Creates the modules. Called once from the client entrypoint. */
    public static void registerAll() {
        WindowLockModule windowLock = new WindowLockModule();
        fireworkCooldown = new FireworkCooldownModule();
        RainSleepModule rainSleep = new RainSleepModule();
        DropKeyModule dropKey = new DropKeyModule();
        FullscreenKeyModule fullscreenKey = new FullscreenKeyModule();
        EffectParticleModule effectParticles = new EffectParticleModule();
        LightPreviewOverlay lightPreview = new LightPreviewOverlay();
        MovingLightModule movingLight = new MovingLightModule();
        SimpleSwitchModule packWarning =
                new SimpleSwitchModule(SwitchIds.RESOURCE_PACK_WARNING_OFF);
        SimpleSwitchModule campfireSmoke = new SimpleSwitchModule(SwitchIds.CAMPFIRE_SMOKE_OFF);
        SimpleSwitchModule tutorial = new SimpleSwitchModule(SwitchIds.TUTORIAL_OFF);
        CarpetFakePlayerModule carpetFakePlayer = new CarpetFakePlayerModule();
        TotemRefillModule totemRefill = new TotemRefillModule();
        SimpleSwitchModule tntSound = new SimpleSwitchModule(SwitchIds.TNT_SOUND_OFF);
        SimpleSwitchModule minecartSound = new SimpleSwitchModule(SwitchIds.MINECART_SOUND_OFF);
        SimpleSwitchModule pistonSound = new SimpleSwitchModule(SwitchIds.PISTON_SOUND_OFF);

        MODULES.put(SwitchIds.WINDOW_LOCK, windowLock);
        MODULES.put(SwitchIds.FIREWORK_COOLDOWN, fireworkCooldown);
        MODULES.put(SwitchIds.RAIN_SLEEP, rainSleep);
        MODULES.put(SwitchIds.DROP_KEY_LOCK, dropKey);
        MODULES.put(SwitchIds.FULLSCREEN_KEY_LOCK, fullscreenKey);
        MODULES.put(SwitchIds.EFFECT_PARTICLES_OFF, effectParticles);
        MODULES.put(SwitchIds.LIGHT_PREVIEW, lightPreview);
        MODULES.put(SwitchIds.MOVING_LIGHT, movingLight);
        MODULES.put(SwitchIds.RESOURCE_PACK_WARNING_OFF, packWarning);
        MODULES.put(SwitchIds.CAMPFIRE_SMOKE_OFF, campfireSmoke);
        MODULES.put(SwitchIds.TUTORIAL_OFF, tutorial);
        MODULES.put(SwitchIds.CARPET_FAKE_PLAYER, carpetFakePlayer);
        MODULES.put(SwitchIds.TOTEM_REFILL, totemRefill);
        MODULES.put(SwitchIds.TNT_SOUND_OFF, tntSound);
        MODULES.put(SwitchIds.MINECART_SOUND_OFF, minecartSound);
        MODULES.put(SwitchIds.PISTON_SOUND_OFF, pistonSound);

        for (SwitchDefinition definition : SwitchIds.all()) {
            ClientFeature feature = MODULES.get(definition.id());
            if (feature == null) {
                MoreSwitchesMod.LOGGER.error("[{}] switch '{}' has no client module and will do nothing",
                        MoreSwitchesMod.MOD_ID, definition.id());
                continue;
            }
            feature.register(definition.id());
        }
    }

    /** Current value of a switch as stored on disk. */
    public static boolean isEnabled(String switchId) {
        return ModSettings.get().isEnabled(switchId);
    }

    /** Stores a new value and applies it immediately. */
    public static void setEnabled(String switchId, boolean enabled) {
        ModSettings.get().setEnabled(switchId, enabled);
        ClientFeature feature = MODULES.get(switchId);
        if (feature != null) {
            feature.onEnabledChanged(enabled);
        } else {
            MoreSwitchesMod.LOGGER.error("[{}] flip of unknown switch '{}' was saved but not applied",
                    MoreSwitchesMod.MOD_ID, switchId);
        }
    }

    /**
     * Mixin bridge: called before vanilla turns a right-click into an item use.
     *
     * @return true when the click must be swallowed
     */
    public static boolean blocksItemUse(Minecraft minecraft, Player player, InteractionHand hand) {
        if (fireworkCooldown == null) {
            return false;
        }
        return fireworkCooldown.blocksUse(minecraft, player, hand);
    }
}
