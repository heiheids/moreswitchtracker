package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.cooldown.CooldownClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Blocks a second firework rocket use for three seconds after the first.
 *
 * <p>Nothing vanilla provides this: fireworks carry no {@code use_cooldown} component, and the
 * vanilla cooldown table only suppresses a use that has already been sent to the server. The
 * decision therefore has to happen <em>before</em> the click is turned into a packet — see
 * {@code MixinMultiPlayerGameMode} — and the hook is a simple "may this use go through?".</p>
 *
 * <p>Only rockets are affected. Every other item, block and entity interaction is untouched
 * because the caller asks {@link #blocksUse} and falls through to vanilla when it says no.</p>
 */
public final class FireworkCooldownModule implements ClientFeature {

    /** Three seconds. */
    private static final long COOLDOWN_MILLIS = 3_000L;

    /** The same window in ticks, used only for the vanilla cooldown overlay. */
    private static final int COOLDOWN_TICKS = 60;

    /** Shared so the screen can show the remaining time without reaching into the module. */
    private static final CooldownClock CLOCK = new CooldownClock(COOLDOWN_MILLIS);

    private boolean active;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        if (!this.active) {
            CLOCK.reset();
        }
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        this.active = enabled;
        if (!enabled) {
            CLOCK.reset();
        }
    }

    /**
     * Decides whether one right-click use must be refused.
     *
     * <p>Called from the mixin at the head of the vanilla use call, i.e. before any packet is
     * built. When it returns false the use proceeds untouched; when it returns true the caller
     * cancels the vanilla method, so nothing is spawned and nothing is sent.</p>
     *
     * @return true when this use has to be swallowed
     */
    public boolean blocksUse(Minecraft minecraft, Player player, InteractionHand hand) {
        if (!this.active || player == null) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.FIREWORK_ROCKET)) {
            return false;
        }

        if (CLOCK.blockIfCooling(System.currentTimeMillis())) {
            MoreSwitchesMod.LOGGER.info("[{}] firework rocket blocked, {}s left",
                    MoreSwitchesMod.MOD_ID, remainingSeconds());
            return true;
        }
        // The use is going through: show the vanilla cooldown bar so the player can see why the
        // next click will be refused.
        applyVisualCooldown(minecraft, stack);
        MoreSwitchesMod.LOGGER.info("[{}] firework rocket used, {}s cooldown started",
                MoreSwitchesMod.MOD_ID, COOLDOWN_MILLIS / 1000L);
        return false;
    }

    /** Remaining block time in whole seconds; 0 when a rocket can be used again. */
    public static int remainingSeconds() {
        return CLOCK.remainingSeconds(System.currentTimeMillis());
    }

    /**
     * Starts the game's own cooldown overlay on the rocket, so the player sees a filling bar
     * instead of a click that silently does nothing.
     *
     * <p>This only touches the client's copy of the cooldown table; it is never sent to the
     * server, whose table stays authoritative and untouched.</p>
     */
    private void applyVisualCooldown(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
    }
}
