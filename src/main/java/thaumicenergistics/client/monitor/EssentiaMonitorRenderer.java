package thaumicenergistics.client.monitor;

import appeng.api.orientation.BlockOrientation;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.util.ReadableNumberConverter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;

public final class EssentiaMonitorRenderer implements EssentiaStorageMonitorPart.Renderer {
    private static final ResourceLocation LOCK_TEXTURE = ThaumicEnergistics.id("textures/block/parts/monitor.locked.png");
    private static final int LIGHT = LightTexture.pack(13, 13);
    private static final float DEPTH = 0.505F;
    private static final float ICON_SIZE = 0.38F;
    private static final float TEXT_SCALE = 1.0F / 62.0F;

    @Override
    public void render(EssentiaStorageMonitorPart part, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Aspect aspect = part.getTrackedAspect();
        if (aspect == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        BlockEntityRenderHelper.rotateToFace(poseStack, BlockOrientation.get(part.getSide(), 0));
        poseStack.translate(0, 0, DEPTH);
        int color = aspect.getColor();
        quad(poseStack, buffers.getBuffer(RenderType.text(aspect.getImage())), -0.2F, 0.25F - ICON_SIZE, -0.2F + ICON_SIZE, 0.25F, 0, 1,
            FastColor.ARGB32.color(229, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color)));
        float lockU = part.isLocked() ? 0.0F : 0.5F;
        poseStack.translate(0, 0, 0.003);
        quad(poseStack, buffers.getBuffer(RenderType.text(LOCK_TEXTURE)), 0.4F, -0.04F, 0.5F, 0.06F, lockU, lockU + 0.5F, 0x80FFFFFF);
        String amount = ReadableNumberConverter.format(part.getTrackedAmount(), 4);
        Font font = Minecraft.getInstance().font;
        poseStack.translate(0, -0.15, 0.003);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        font.drawInBatch(amount, -font.width(amount) / 2.0F, 0, 0xFF000000, false, poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LIGHT);
        poseStack.popPose();
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer, float x0, float y0, float x1, float y1, float u0, float u1, int color) {
        Matrix4f pose = poseStack.last().pose();
        consumer.addVertex(pose, x0, y0, 0).setColor(color).setUv(u0, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y0, 0).setColor(color).setUv(u1, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y1, 0).setColor(color).setUv(u1, 0).setLight(LIGHT);
        consumer.addVertex(pose, x0, y1, 0).setColor(color).setUv(u0, 0).setLight(LIGHT);
    }
}
