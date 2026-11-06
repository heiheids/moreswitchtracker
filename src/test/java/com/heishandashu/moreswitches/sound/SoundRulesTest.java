package com.heishandashu.moreswitches.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sound identifier recognition.
 *
 * <p>The interesting cases are the ones that must <em>not</em> match: an unrelated sound, and the
 * IDs that merely look similar. A typo in one of these identifiers would silently silence nothing,
 * which is the failure this pins down.</p>
 */
class SoundRulesTest {

    @Test
    void everyConfiguredIdentifierIsRecognised() {
        assertTrue(SoundRules.isKnown(SoundRules.EXPLODE));
        assertTrue(SoundRules.isKnown(SoundRules.MINECART_RIDING));
        assertTrue(SoundRules.isKnown(SoundRules.MINECART_INSIDE));
        assertTrue(SoundRules.isKnown(SoundRules.MINECART_INSIDE_UNDERWATER));
        assertTrue(SoundRules.isKnown(SoundRules.PISTON_EXTEND));
        assertTrue(SoundRules.isKnown(SoundRules.PISTON_CONTRACT));
    }

    @Test
    void unrelatedSoundsAreNeverRecognised() {
        assertFalse(SoundRules.isKnown("minecraft:block.stone.break"));
        assertFalse(SoundRules.isKnown("minecraft:entity.creeper.primed"));
        assertFalse(SoundRules.isKnown(""));
        assertFalse(SoundRules.isKnown(null));
    }

    @Test
    void aSimilarButDifferentNameIsNotMistakenForTheRealOne() {
        // "explode" versus "explosion" is exactly the kind of near miss that would silence nothing.
        assertFalse(SoundRules.isKnown("minecraft:entity.generic.explosion"));
        assertFalse(SoundRules.isKnown("minecraft:entity.minecart.riding.extra"));
        assertFalse(SoundRules.isKnown("block.piston.extend"),
                "the namespace is part of the identifier");
    }
}
