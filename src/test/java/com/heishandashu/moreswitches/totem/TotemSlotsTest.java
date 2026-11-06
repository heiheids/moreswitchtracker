package com.heishandashu.moreswitches.totem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slot arithmetic for the totem refill.
 *
 * <p>These slots are the part that can quietly put a totem in the wrong place, so the mapping
 * between the player's inventory indices and the menu slots the server understands is pinned
 * here.</p>
 */
class TotemSlotsTest {

    @Test
    void hotbarIndicesMapToTheMenuSlotsTheServerUses() {
        assertEquals(36, TotemSlots.menuSlotFor(0), "first hotbar slot");
        assertEquals(44, TotemSlots.menuSlotFor(8), "last hotbar slot");
    }

    @Test
    void mainInventoryIndicesKeepTheirOwnNumber() {
        assertEquals(9, TotemSlots.menuSlotFor(9), "first main inventory slot");
        assertEquals(35, TotemSlots.menuSlotFor(35), "last main inventory slot");
    }

    @Test
    void onlyTheFirstNineIndicesCountAsHotbar() {
        assertTrue(TotemSlots.isHotbar(0));
        assertTrue(TotemSlots.isHotbar(8));
        assertFalse(TotemSlots.isHotbar(9));
        assertFalse(TotemSlots.isHotbar(35));
    }

    @Test
    void anEmptyHotbarSlotIsPreferredAsStagingPoint() {
        boolean[] allFull = { true, true, true, true, true, true, true, true, true };
        boolean[] thirdFree = { true, true, false, true, true, true, true, true, true };

        assertEquals(0, TotemSlots.stagingHotbarIndex(new boolean[9]),
                "the first free slot wins");
        assertEquals(2, TotemSlots.stagingHotbarIndex(thirdFree));
        assertEquals(8, TotemSlots.stagingHotbarIndex(allFull),
                "with no free slot the last one is used so the swap still works");
    }
}
