package com.heishandashu.moreswitches.client.outline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

import java.util.OptionalDouble;

/**
 * 1.21.1 的高对比度方块轮廓绘制。
 *
 * <p>这一代原版没有该功能，所以自己画，画法跟后来的原版一致：<b>同一个形状画两遍</b>，
 * 一遍粗黑线做衬底、一遍细彩色线盖在上面。线宽用自定义 {@link RenderType} 的
 * {@code LineStateShard} 指定——MC 的 {@code rendertype_lines} 着色器在顶点着色器里按
 * {@code LineWidth} 扩张线条，不依赖被 core profile 钳制的 {@code glLineWidth}。</p>
 *
 * <p>早先的实现把黑衬底做成「外扩的几何体」，它与彩色线深度几乎相同，深度测试互相咬合，
 * 于是出现一段一段的断线。现在两者几何完全一致、深度相等，后画的彩色线用 LEQUAL 稳定覆盖，
 * 不会再闪。</p>
 */
@EventBusSubscriber(value = net.neoforged.api.distmarker.Dist.CLIENT,
        modid = com.heishandashu.moreswitches.MoreSwitchesMod.MOD_ID)
public final class HighContrastOutlineRenderer {

    /** 黑色衬底线宽（屏幕像素）。 */
    private static final double BACKING_WIDTH = 8.0D;

    /** 彩色描边线宽（屏幕像素）。 */
    private static final double LINE_WIDTH = 2.8D;

    private static final float BACKING_ALPHA = 0.85F;
    private static final float LINE_ALPHA = 1.0F;

    /**
     * 粗黑衬底。状态组合与 {@code RenderType.lines()} 相同，只把线宽换成 8 像素。
     */
    private static final RenderType BACKING = RenderType.create(
            "moreswitchtracker_high_contrast_backing",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(BACKING_WIDTH)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    // 只写颜色、不写深度：衬底与彩色线是同一几何、不同线宽，着色器按线宽在屏幕空间
                    // 展开顶点，两者插值深度会有细微差异。衬底若写深度就会和彩色线抢深度，表现为闪烁。
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    /**
     * 彩色细线。几何与衬底完全相同，深度相等，后画所以稳定覆盖。
     */
    private static final RenderType COLOR = RenderType.create(
            "moreswitchtracker_high_contrast_color",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(LINE_WIDTH)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private HighContrastOutlineRenderer() {
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        if (!HighContrastOutline.enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockHitResult target = event.getTarget();
        BlockPos pos = target.getBlockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        VoxelShape shape = state.getShape(minecraft.level, pos,
                CollisionContext.of(minecraft.player));
        if (shape.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();

        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        renderShape(poseStack, buffers.getBuffer(BACKING), shape, x, y, z,
                0.0F, 0.0F, 0.0F, BACKING_ALPHA);

        int argb = HighContrastOutline.color();
        renderShape(poseStack, buffers.getBuffer(COLOR), shape, x, y, z,
                ((argb >> 16) & 0xFF) / 255.0F,
                ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F,
                LINE_ALPHA);
    }

    /**
     * 画一个形状的轮廓线。
     *
     * <p>顶点格式必须是 {@code POSITION_COLOR_NORMAL}：线着色器用 {@code Position + Normal}
     * 求另一端的投影方向，所以法线要传这条边的单位方向，而不是多边形法线。</p>
     */
    private static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape,
                                    double x, double y, double z,
                                    float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = net.minecraft.util.Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-5F) {
                return;
            }
            float nx = dx / length;
            float ny = dy / length;
            float nz = dz / length;
            consumer.addVertex(pose, (float) (x1 + x), (float) (y1 + y), (float) (z1 + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, nx, ny, nz);
            consumer.addVertex(pose, (float) (x2 + x), (float) (y2 + y), (float) (z2 + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, nx, ny, nz);
        });
    }
}
