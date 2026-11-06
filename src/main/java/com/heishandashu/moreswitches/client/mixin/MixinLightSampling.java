package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.feature.MovingLightModule;
import com.heishandashu.moreswitches.client.light.ClientHeldLight;
import com.heishandashu.moreswitches.light.LightPreviewRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a held light source appear to light the blocks around it.
 *
 * <p>The target is the brightness lookup the section builder goes through for every block face. The
 * injection rewrites the packed light of that one position: the sky light is preserved and only the
 * block light is raised, so the result looks like a torch was placed there rather than like the whole
 * screen got brighter.</p>
 *
 * <p>The packed layout on 1.21.1 is {@code block << 4 | sky << 20}, the same as vanilla has always
 * used, and that is why only the block half is replaced.</p>
 *
 * <p>Two properties matter for correctness:</p>
 * <ul>
 *   <li>it is per position, so the brightening falls off with distance exactly like vanilla block
 *       light instead of glowing evenly;</li>
 *   <li>it only changes what the renderer is told — nothing is written to the level, the light engine
 *       or any saved data, so the world's real light is untouched.</li>
 * </ul>
 *
 * <p>Because these values are baked into the section meshes, this injection alone is not enough to
 * make the light appear; {@code MovingLightModule} re-meshes the nearby sections when the light
 * moves.</p>
 */
@Mixin(LevelRenderer.class)
public class MixinLightSampling {

    private static final int BLOCK_MASK = 0xFF00;
    private static final int SKY_MASK = 0xFF0000;

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, require = 1)
    private static void moreswitches$addHeldLight(BlockAndTintGetter level, BlockState state,
                                                  BlockPos pos,
                                                  CallbackInfoReturnable<Integer> cir) {
        if (!LightPreviewRule.movingLightEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // Note: `level` must not be compared with `minecraft.level`. While a section is being meshed
        // the caller passes a render region snapshot, not the level itself, so an identity check
        // rejects every real call and the light never gets baked.
        int emission = ClientHeldLight.emission(minecraft);
        if (emission <= 0) {
            return;
        }
        Vec3 centre = ClientHeldLight.centre(minecraft);
        int simulated = ClientHeldLight.levelAt(minecraft, pos, emission, centre);
        if (simulated <= 0) {
            return;
        }

        int packed = cir.getReturnValueI();
        int existing = (packed & BLOCK_MASK) >> 4;
        if (simulated > existing) {
            // Keep the sky half untouched so daylight still reads as daylight.
            int sky = packed & SKY_MASK;
            cir.setReturnValue(sky | (simulated << 4));
            MovingLightModule.noteBakedMesh();
        }
    }
}
