package com.heishandashu.moreswitches.client.attackrange;

import com.heishandashu.moreswitches.config.ModSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 攻击距离高亮：手持近战武器时，把武器能打到的怪物点亮成光灵箭那种白色发光轮廓。
 *
 * <h2>为什么直接借用原版的发光轮廓</h2>
 *
 * <p>光灵箭的高亮就是原版的实体发光轮廓。{@code Minecraft} 每帧为每个实体调用
 * {@link Minecraft#shouldEntityAppearGlowing(Entity)}，结果被 {@code EntityRenderer} 写进
 * {@code EntityRenderState.outlineColor}。这里只回答「这个怪物该不该亮」，真正的绘制、
 * 遮挡处理、队伍颜色全部交给原版，所以显示效果和光灵箭完全一致。</p>
 *
 * <h2>范围判定（1.21.1）</h2>
 *
 * <p>只看<b>怪物离玩家有多远</b>，与鼠标指向、视线方向都无关：取眼睛到碰撞箱最近点的距离，
 * 不超过实体交互距离 {@code entityInteractionRange()}（生存 3 格、创造 5 格，受属性加成影响）
 * 就亮。改动会在下一帧立刻生效，不缓存任何状态。</p>
 *
 * <p>唯一和视野有关的是原版的绘制流程本身：只有进入画面的实体才会被渲染，所以屏幕外的怪物
 * 即使在高亮范围内也没有可画的地方。这一点和光灵箭完全一样。</p>
 *
 * <h2>只标记怪物</h2>
 *
 * <p>实体必须是 {@link Mob} 且实现 {@link Enemy}（原版「敌对生物」标记），所以僵尸、骷髅、
 * 苦力怕、蜘蛛、猪灵等会亮，村民、动物、铁傀儡、玩家都不会。手里必须是剑、斧、三叉戟或重锤，
 * 所以空手、弓、弩、食物、方块都不会触发。</p>
 */
public final class AttackRangeHighlight {

    /** 留一点余量，避免两帧之间怪物挪动导致边界闪烁；只收窄范围，不会放宽。 */
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
        double distance = Math.sqrt(entity.getBoundingBox().distanceToSqr(player.getEyePosition()));
        return distance <= player.entityInteractionRange() - SAFETY_MARGIN;
    }

    /** 剑、斧、三叉戟、重锤是这一代原版的近战武器。 */
    private static boolean isMeleeWeapon(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE));
    }
}
