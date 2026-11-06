package com.heishandashu.moreswitches.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the rule layer that both light switches sit on.
 *
 * <p>Covers the questions the mixins actually ask: is a held block worth simulating, and how much
 * light would it add at a given distance.</p>
 */
class LightPreviewRuleTest {

    @Test
    void onlyBlocksThatEmitLightCountAsSources() {
        assertTrue(LightPreviewRule.isLightSource(14), "a torch is a source");
        assertTrue(LightPreviewRule.isLightSource(1), "even a single level counts");
        assertFalse(LightPreviewRule.isLightSource(0), "a stone block is not a source");
        assertFalse(LightPreviewRule.isLightSource(-1));
    }

    @Test
    void addedLightFollowsTheSameFalloff() {
        assertEquals(14, LightPreviewRule.addedLight(14, 0.0D));
        assertEquals(10, LightPreviewRule.addedLight(14, 4.0D));
        assertEquals(0, LightPreviewRule.addedLight(14, 14.0D));
    }

    @Test
    void aNonSourceAddsNothingAtAnyDistance() {
        assertEquals(0, LightPreviewRule.addedLight(0, 0.0D));
        assertEquals(0, LightPreviewRule.addedLight(0, 5.0D));
    }
}
