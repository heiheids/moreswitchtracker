package com.heishandashu.moreswitches.light;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Works out how much light a held block would emit, and how much of it reaches a position.
 *
 * <p>This half lives in the common source set, so it can only touch types that exist on both
 * sides: the player, the held stack, the level and its blocks. Everything that needs the client
 * (the camera, the crosshair target, the settings switch) lives in {@code ClientHeldLight}, which
 * calls into here.</p>
 *
 * <p>Nothing in this class writes to the level. The values are only ever handed to the renderer.</p>
 */
public final class HeldLight {

    private HeldLight() {
    }

    /** The light level the held item would emit, or 0 when it is not a light source. */
    public static int heldEmission(Player player) {
        if (player == null) {
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return 0;
        }
        BlockState state = Block.byItem(held.getItem()).defaultBlockState();
        return state.getLightEmission();
    }

    /**
     * Light the held source would put on a block, before the world's own light is considered.
     *
     * <p>Measured from the block's centre so the numbers line up with what the player sees. Zero
     * when nothing is held, or when the block is out of reach — the caller can then skip its work
     * for the vast majority of blocks.</p>
     */
    public static int simulatedLevelAt(Level level, BlockPos pos, int emission, Vec3 centre) {
        if (emission <= 0 || centre == null || level == null) {
            return 0;
        }
        return LightPreviewRule.addedLight(emission, centre.distanceTo(Vec3.atCenterOf(pos)));
    }
}
