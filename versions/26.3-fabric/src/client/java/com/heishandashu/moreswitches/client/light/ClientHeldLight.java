package com.heishandashu.moreswitches.client.light;

import com.heishandashu.moreswitches.light.HeldLight;
import com.heishandashu.moreswitches.light.LightPreviewRule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The client half of the light simulation: where the light sits, and what the held item emits.
 *
 * <p>The centre is chosen the way a placement would be, but nothing is placed. A targeted block
 * wins, and the light sits where a torch would stand on it — on top of the targeted block, or in
 * the air block just beyond the face that was hit so a torch on a wall reads correctly. With no
 * target the light floats a short distance in front of the eyes, which is what makes it follow the
 * player instead of only working while staring at a wall.</p>
 */
public final class ClientHeldLight {

    /** How far in front of the eyes the light floats when nothing is targeted. */
    private static final double AIR_OFFSET = 1.5D;

    private ClientHeldLight() {
    }

    /** Light level the held item would emit, or 0 when it is not a light source or both switches are off. */
    public static int emission(Minecraft minecraft) {
        if (minecraft == null || !LightPreviewRule.anyEnabled()) {
            return 0;
        }
        return HeldLight.heldEmission(minecraft.player);
    }

    /**
     * Where the simulated light is centred, or null when there is nothing to show.
     *
     * <p>Uses the crosshair target when there is one, so the player can line the light up with a
     * spot on a wall or the ground. Falls back to a point in front of the eyes.</p>
     */
    public static Vec3 centre(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return null;
        }
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockPos target = minecraft.level.getBlockState(pos).canBeReplaced()
                    ? pos
                    : pos.relative(blockHit.getDirection());
            return Vec3.atCenterOf(target);
        }
        Vec3 eyes = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getViewVector(1.0F).normalize().scale(AIR_OFFSET);
        return eyes.add(look);
    }

    /** Simulated light the held source would put on a specific block. */
    public static int levelAt(Minecraft minecraft, BlockPos pos, int emission, Vec3 centre) {
        if (minecraft == null || minecraft.level == null) {
            return 0;
        }
        return HeldLight.simulatedLevelAt(minecraft.level, pos, emission, centre);
    }
}
