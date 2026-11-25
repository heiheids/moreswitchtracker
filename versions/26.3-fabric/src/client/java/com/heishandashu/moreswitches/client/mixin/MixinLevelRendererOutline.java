package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.outline.HighContrastOutline;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 把原版高对比度方块轮廓的彩色细线换成设置里选的颜色。
 *
 * <p>26.3 的流程是：{@code submitBlockOutline} 计算颜色，再交给 {@code submitHitOutline}
 * 提交几何；颜色就是这个方法的第 5 个参数。只改参数，不接管绘制。</p>
 *
 * <p>判断依据是颜色值本身：只有恰好等于原版高对比度色 {@code 0xFF57FFE1} 的那条线会被替换，
 * 黑色衬底（{@code 0xFF000000}）、非高对比度模式的半透明黑边（{@code 0x66000000}）、
 * 调试形状的白色（{@code 0xFFFFFFFF}）都不受影响。</p>
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRendererOutline {

    @ModifyVariable(
            method = "submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/rendertype/RenderType;"
                    + "Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;"
                    + "IFZ)V",
            at = @At("HEAD"), argsOnly = true, require = 1)
    private int moreswitches$customOutlineColor(int color) {
        return HighContrastOutline.colorFor(color);
    }
}
