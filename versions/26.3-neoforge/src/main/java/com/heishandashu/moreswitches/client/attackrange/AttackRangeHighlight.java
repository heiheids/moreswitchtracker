package com.heishandashu.moreswitches.client.attackrange;

import com.heishandashu.moreswitches.config.ModSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;

/**
 * 攻击距离高亮：手持近战武器时，把武器能打到的怪物点亮成光灵箭那种白色发光轮廓。
 *
 * <h2>为什么直接借用原版的发光轮廓</h2>
 *
 * <p>光灵箭的高亮就是原版的实体发光轮廓（{@code entity_outline} 后处理）。{@code Minecraft}
 * 每帧为每个实体调用 {@link Minecraft#shouldEntityAppearGlowing(Entity)}，结果被
 * {@code EntityRenderer} 写成 {@code EntityRenderState.outlineColor}。这里只回答「这个怪物该不该亮」，
 * 真正的绘制、遮挡处理、队伍颜色全部交给原版，所以显示效果和光灵箭完全一致，也不会和光影冲突。</p>
 *
 * <h2>范围判定</h2>
 *
 * <p>只看<b>怪物离玩家有多远</b>，与鼠标指向、视线方向都无关：取眼睛到碰撞箱最近点的距离，
 * 落在武器自己的攻击距离内就亮。攻击距离来自 26.3 的 {@code DataComponents.ATTACK_RANGE}
 * （{@code AttackRange.effectiveMaxRange / effectiveMinRange}）：剑 3 格、矛 4.5 格、
 * 创造模式 5 格，并且保留矛的「太近反而打不到」最小距离。</p>
 *
 * <p>判定是每次渲染都实时算的，不缓存任何状态：怪物一走出攻击距离，下一帧就不再被标记，
 * 不存在高亮残留或延迟。</p>
 *
 * <p>唯一和视野有关的是原版的绘制流程本身：只有进入画面的实体才会被渲染，所以屏幕外的怪物
 * 即使在高亮范围内也没有可画的地方。这一点和光灵箭完全一样。</p>
 *
 * <h2>只标记怪物</h2>
 *
 * <p>实体必须是 {@link Mob} 且实现 {@link Enemy}（原版「敌对生物」标记），所以僵尸、骷髅、苦力怕、
 * 蜘蛛、猪灵等会亮，村民、动物、铁傀儡、玩家都不会。手里必须是带
 * {@code DataComponents.WEAPON} 的近战武器（剑、斧、矛、三叉戟、重锤等），所以空手、弓、弩、
 * 食物、方块都不会触发；远程武器本身也不在「近战攻击」范围内。</p>
 */
public final class AttackRangeHighlight {

    /**
     * 留一点余量，避免距离刚好卡在边界上时因为怪物在两帧之间挪动而闪烁。
     *
     * <p>它只把攻击距离<b>收窄</b>，不会让够不到的怪物亮起来。</p>
     */
    public static final double SAFETY_MARGIN = 0.05;

    private AttackRangeHighlight() {
    }

    /**
     * Mixin 入口：这个实体这一帧是否应该显示光灵箭式高亮。
     *
     * @return true 表示强制显示发光轮廓；false 表示交回原版判断
     */
    public static boolean shouldGlow(Entity entity) {
        if (!ModSettings.get().attackRangeHighlight()) {
            return false;
        }
        return canReach(entity);
    }

    private static boolean canReach(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return false;
        }
        // 只标记敌对怪物：村民、动物、傀儡、玩家都被这一条挡掉。
        if (!(entity instanceof Mob) || !(entity instanceof Enemy)) {
            return false;
        }
        if (entity == player || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        if (entity.level() != minecraft.level) {
            return false;
        }
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }

        ItemStack weapon = player.getMainHandItem();
        if (!isMeleeWeapon(weapon)) {
            return false;
        }
        AttackRange range = player.getAttackRangeWith(weapon);
        double distance = Math.sqrt(entity.getBoundingBox().distanceToSqr(player.getEyePosition()));
        return distance <= range.effectiveMaxRange(player) - SAFETY_MARGIN
                && distance >= range.effectiveMinRange(player) + SAFETY_MARGIN;
    }

    /**
     * 原版给所有能当近战武器用的物品都挂了 {@code WEAPON} 组件（剑、斧、矛、三叉戟、重锤，
     * 工具类也会挂），而弓、弩、食物、方块没有。用它判断「手里拿的是不是近战武器」与这代原版
     * 自己的定义一致，也不依赖物品 id 列表。
     */
    private static boolean isMeleeWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.WEAPON);
    }
}
