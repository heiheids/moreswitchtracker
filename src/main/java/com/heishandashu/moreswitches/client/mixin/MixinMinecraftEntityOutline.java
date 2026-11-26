package com.heishandashu.moreswitches.client.mixin;

import com.heishandashu.moreswitches.client.attackrange.AttackRangeHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把「攻击距离高亮」接到原版的发光轮廓判断上。
 *
 * <p>实体轮廓只由 {@link Minecraft#shouldEntityAppearGlowing(Entity)} 决定；这里只在判定为
 * true 时接管，原版的发光逻辑（光灵箭、怪物透视的服务端发光标记、旁观者轮廓）保持原样。</p>
 *
 * <p>{@code shouldEntityAppearGlowing} 每帧每实体调用一次，所以高亮和清除都是实时的。
 * {@code require = 1} 让方法名对不上时直接启动失败，而不是静默失效。</p>
 */
@Mixin(Minecraft.class)
public class MixinMinecraftEntityOutline {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 1)
    private void moreswitches$attackRangeHighlight(Entity entity,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (AttackRangeHighlight.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
