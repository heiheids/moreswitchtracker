package com.heishandashu.moreswitches.light;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour of the simulated light falloff.
 *
 * <p>These pin the arithmetic that makes the preview look like a real torch: one level lost per
 * block, nothing beyond the source's own level, and no stacking on top of existing light.</p>
 */
class LightFalloffTest {

    @Test
    void aTorchIsFullStrengthRightNextToIt() {
        assertEquals(14, LightFalloff.levelAt(14, 0.0D), "at the source the level is the emission");
    }

    @Test
    void oneLevelIsLostPerBlock() {
        assertEquals(13, LightFalloff.levelAt(14, 1.0D));
        assertEquals(12, LightFalloff.levelAt(14, 2.0D));
        assertEquals(1, LightFalloff.levelAt(14, 13.0D));
    }

    @Test
    void thereIsNothingLeftAtTheEmissionDistance() {
        assertEquals(0, LightFalloff.levelAt(14, 14.0D),
                "14 blocks away a torch contributes nothing, exactly as in vanilla");
        assertEquals(0, LightFalloff.levelAt(14, 40.0D));
    }

    @Test
    void reachStopsBeforeTheLightDiesOut() {
        assertEquals(13, LightFalloff.reach(14), "a torch lights 13 blocks out");
        assertEquals(5, LightFalloff.reach(6), "a soul torch (10) lights further than a candle (3)");
        assertEquals(0, LightFalloff.reach(1));
    }

    @Test
    void inReachMatchesTheLevel() {
        assertTrue(LightFalloff.inReach(14, 13.0D));
        assertFalse(LightFalloff.inReach(14, 14.0D));
    }

    @Test
    void lightDoesNotStackWithWhatIsAlreadyThere() {
        assertEquals(9, LightFalloff.brighterOf(9, 5), "an already brighter spot keeps its level");
        assertEquals(7, LightFalloff.brighterOf(3, 7), "the simulated source wins when it is brighter");
        assertEquals(7, LightFalloff.brighterOf(7, 7));
    }

    @Test
    void aNonLightEmitsNothing() {
        assertEquals(0, LightFalloff.levelAt(0, 0.0D));
        assertFalse(LightFalloff.inReach(0, 0.0D));
    }
}
