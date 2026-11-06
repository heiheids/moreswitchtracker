package com.heishandashu.moreswitches.client.feature;

import com.heishandashu.moreswitches.MoreSwitchesMod;
import com.heishandashu.moreswitches.client.ClientTickHook;
import com.heishandashu.moreswitches.config.SwitchIds;
import com.heishandashu.moreswitches.totem.TotemSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Puts a fresh totem of undying in the offhand whenever the offhand no longer holds one.
 *
 * <p>The move is done with ordinary container clicks rather than by editing the inventory
 * directly, because the server owns the inventory: writing to it on the client would be undone on
 * the next sync and could desynchronise the session. A swap click is exactly what a player does by
 * hand, so the server accepts it without any special handling.</p>
 *
 * <p>A swap can only exchange a hotbar slot with the offhand. A totem found in the main inventory
 * therefore takes two clicks: first into a hotbar slot, then into the offhand. An empty hotbar slot
 * is preferred as the staging point, which keeps the disturbance to the player's hotbar minimal.</p>
 *
 * <p>Refills are spaced out and never happen while a screen is open, so the mod cannot fight the
 * player's own clicks or spam the server when there is no totem to move.</p>
 */
public final class TotemRefillModule implements ClientFeature {

    /** Ticks to wait between attempts; a refill is not urgent enough to try every tick. */
    private static final int RETRY_TICKS = 10;

    private boolean active;

    private int cooldown;

    @Override
    public void register(String switchId) {
        this.active = enabled(switchId);
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' starts {}", MoreSwitchesMod.MOD_ID,
                SwitchIds.TOTEM_REFILL, this.active ? "on" : "off");
        ClientTickHook.onEndTick(this::tick);
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (this.active == enabled) {
            return;
        }
        this.active = enabled;
        this.cooldown = 0;
        MoreSwitchesMod.LOGGER.info("[{}] switch '{}' -> {}",
                MoreSwitchesMod.MOD_ID, SwitchIds.TOTEM_REFILL, enabled);
    }

    private void tick(Minecraft client) {
        if (!this.active) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.screen != null) {
            return;
        }
        // Only touch a container the player is not already using.
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }
        if (isTotem(player.getOffhandItem())) {
            return;
        }

        int source = findTotemSlot(player);
        if (source < 0) {
            this.cooldown = RETRY_TICKS;
            return;
        }

        if (TotemSlots.isHotbar(source)) {
            swapWithOffhand(client, player, TotemSlots.menuSlotFor(source));
        } else {
            int staging = TotemSlots.stagingHotbarIndex(hotbarOccupied(player));
            swapSlots(client, player, TotemSlots.menuSlotFor(source), staging);
            swapWithOffhand(client, player, TotemSlots.HOTBAR_START + staging);
        }
        this.cooldown = RETRY_TICKS;
    }

    private static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    /** Inventory index of the first totem outside the offhand, or -1. */
    private static int findTotemSlot(LocalPlayer player) {
        for (int index = 0; index < TotemSlots.HOTBAR_START; index++) {
            if (isTotem(player.getInventory().getItem(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean[] hotbarOccupied(LocalPlayer player) {
        boolean[] occupied = new boolean[TotemSlots.HOTBAR_SIZE];
        for (int index = 0; index < TotemSlots.HOTBAR_SIZE; index++) {
            occupied[index] = !player.getInventory().getItem(index).isEmpty();
        }
        return occupied;
    }

    /** Swaps that hotbar-ish slot with the offhand, exactly like pressing F. */
    private static void swapWithOffhand(Minecraft client, LocalPlayer player, int menuSlot) {
        client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot,
                TotemSlots.SWAP_TO_OFFHAND, ClickType.SWAP, player);
    }

    /** Swaps a menu slot with a hotbar slot, used to stage a totem for the offhand swap. */
    private static void swapSlots(Minecraft client, LocalPlayer player, int menuSlot, int hotbarIndex) {
        client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot,
                hotbarIndex, ClickType.SWAP, player);
    }
}
