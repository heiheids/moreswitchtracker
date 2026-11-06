package com.heishandashu.moreswitches.client.light;

import com.heishandashu.moreswitches.client.feature.MovingLightModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

/**
 * Shared body of the moving-light injections.
 *
 * <p>26.3 has two light-coordinate entry points used by chunk meshing:
 * {@code BlockModelLighter.getLightCoords} for flat lighting and
 * {@code BlockModelLighter.Cache.getLightCoords} for ambient-occlusion lighting. Both mixins
 * delegate here so one feature cannot work in one graphics mode and silently do nothing in the
 * other.</p>
 */
public final class HeldLightPacker {

    private HeldLightPacker() {
    }

    /** Returns the packed light with the simulated held-light added, or the input unchanged. */
    public static int apply(int packed, BlockPos pos) {
        MovingLightModule.noteSampled();
        if (!com.heishandashu.moreswitches.light.LightPreviewRule.movingLightEnabled()) {
            return packed;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return packed;
        }
        // The `level` argument of the light lookup is a render-region snapshot, so it must not be
        // compared with minecraft.level; the simulated source uses the real level instead.
        int emission = ClientHeldLight.emission(minecraft);
        if (emission <= 0) {
            return packed;
        }
        Vec3 centre = ClientHeldLight.centre(minecraft);
        int simulated = ClientHeldLight.levelAt(minecraft, pos, emission, centre);
        if (simulated <= 0 || simulated <= LightCoordsUtil.block(packed)) {
            return packed;
        }
        MovingLightModule.noteBakedMesh();
        return LightCoordsUtil.withBlock(packed, simulated);
    }
}
