package com.heishandashu.moreswitches.totem;

import com.heishandashu.moreswitches.config.ModSettings;
import com.heishandashu.moreswitches.config.SwitchIds;

/**
 * Decides which inventory slot a replacement totem should come from.
 *
 * <p>Kept free of Minecraft types so the slot arithmetic — the part that can silently put an item
 * in the wrong place — is testable without a running game.</p>
 */
public final class TotemSlots {

    /** First slot of the hotbar in the player's inventory menu. */
    public static final int HOTBAR_START = 36;

    /** Last slot of the hotbar (inclusive) in the player's inventory menu. */
    public static final int HOTBAR_END = 44;

    /** The offhand slot in the player's inventory menu. */
    public static final int OFFHAND = 45;

    /** Swap button value that targets the offhand slot. */
    public static final int SWAP_TO_OFFHAND = 40;

    /** Number of hotbar slots. */
    public static final int HOTBAR_SIZE = 9;

    private TotemSlots() {
    }

    /** Whether the auto-refill switch is on. */
    public static boolean enabled() {
        return ModSettings.get().isEnabled(SwitchIds.TOTEM_REFILL);
    }

    /**
     * Menu slot for an inventory index.
     *
     * <p>Indices 0..8 are the hotbar and land at 36..44; indices 9..35 are the main inventory and
     * keep their own number. Armour and crafting slots are never returned, so a totem can only be
     * taken from a place a player could also have dragged it out of by hand.</p>
     */
    public static int menuSlotFor(int inventoryIndex) {
        if (inventoryIndex >= 0 && inventoryIndex < HOTBAR_SIZE) {
            return HOTBAR_START + inventoryIndex;
        }
        return inventoryIndex;
    }

    /** Whether that inventory index is in the hotbar, so it can be swapped to the offhand directly. */
    public static boolean isHotbar(int inventoryIndex) {
        return inventoryIndex >= 0 && inventoryIndex < HOTBAR_SIZE;
    }

    /**
     * Which hotbar index to route a main-inventory totem through.
     *
     * <p>An empty hotbar slot is preferred so the two-step swap disturbs as little as possible; the
     * last slot is used otherwise. Routing through the hotbar is necessary because a swap can only
     * exchange a hotbar slot with the offhand, never an arbitrary inventory slot.</p>
     *
     * @param hotbarOccupied nine flags, true when that hotbar slot holds something
     */
    public static int stagingHotbarIndex(boolean[] hotbarOccupied) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (i < hotbarOccupied.length && !hotbarOccupied[i]) {
                return i;
            }
        }
        return HOTBAR_SIZE - 1;
    }
}
