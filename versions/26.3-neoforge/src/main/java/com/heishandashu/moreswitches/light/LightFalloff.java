package com.heishandashu.moreswitches.light;

/**
 * Distance falloff for the simulated light sources.
 *
 * <p>Minecraft's own block light loses one level per block travelled, so a torch with emission 14
 * reaches exactly 14 blocks and the brightness of a position is {@code 14 - distance}. This class
 * reproduces that same arithmetic for a light that is not actually in the world, which is what
 * makes the preview feel like a real torch instead of a glowing blob.</p>
 *
 * <p>It is deliberately free of Minecraft types: every value it needs is a plain number, so the
 * falloff, the reach and the "is this position affected at all" question can all be unit tested
 * without a running game.</p>
 */
public final class LightFalloff {

    private LightFalloff() {
    }

    /**
     * Light level a source of {@code emission} would leave at {@code distance} blocks away.
     *
     * <p>Matches vanilla: one level lost per block, and nothing beyond the source's own level.
     * The Euclidean distance is rounded down, then the level is {@code emission - distance}. A
     * diagonal step therefore keeps as much light as a straight one, which is how vanilla's
     * flood fill behaves on the diagonal too.</p>
     *
     * @param emission the light level the block would emit, 0..15
     * @param distance distance from the source in blocks
     * @return the resulting light level, 0 when out of reach
     */
    public static int levelAt(int emission, double distance) {
        if (emission <= 0 || distance < 0) {
            return 0;
        }
        int steps = (int) Math.floor(distance);
        int level = emission - steps;
        return Math.max(0, Math.min(15, level));
    }

    /**
     * Whether a position is inside the source's reach at all.
     *
     * <p>Used to decide which blocks get a number drawn on them, so the numbers mark exactly the
     * area the simulated torch would light.</p>
     */
    public static boolean inReach(int emission, double distance) {
        return levelAt(emission, distance) > 0;
    }

    /**
     * The light level to add on top of what the world already has.
     *
     * <p>Light does not stack in vanilla: two torches next to each other are not brighter than
     * one. Taking the maximum rather than summing keeps the preview consistent with that, and it
     * means an already-bright spot is never made brighter than the simulated source itself.</p>
     *
     * @param existing the light level already at that position
     * @param simulated the level the simulated source would put there
     */
    public static int brighterOf(int existing, int simulated) {
        return Math.max(existing, simulated);
    }

    /**
     * Reach in blocks for a given emission level, i.e. the last distance that still has light.
     *
     * <p>A torch (14) reaches 13 blocks: at 14 blocks away the level is 0, which is unlit.</p>
     */
    public static int reach(int emission) {
        return Math.max(0, emission - 1);
    }
}
