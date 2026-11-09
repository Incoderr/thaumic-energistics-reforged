package thaumicenergistics.client.visrelay;

import appeng.api.orientation.BlockOrientation;
import appeng.client.render.BlockEntityRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.visrelay.VisRelayInterfacePart;

/**
 * Draws the drained aspect color over the interface face.
 */
public final class VisRelayInterfaceRenderer implements VisRelayInterfacePart.Renderer {
    private static final ResourceLocation RUNES_TEXTURE = ThaumicEnergistics.id("textures/block/parts/vis.interface.runes.png");
    private static final int LIGHT = LightTexture.pack(13, 13);
    private static final float DEPTH = 0.4345F;
    private static final float HALF_SIZE = 0.25F;

    @Override
    public void render(VisRelayInterfacePart part, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        BlockEntityRenderHelper.rotateToFace(poseStack, BlockOrientation.get(part.getSide(), 0));
        poseStack.translate(0, 0, DEPTH);
        int color = 0xFF000000 | (part.getDrainColor() & 0xFFFFFF);
        quad(poseStack, buffers.getBuffer(RenderType.text(RUNES_TEXTURE)), -HALF_SIZE, -HALF_SIZE, HALF_SIZE, HALF_SIZE, color);
        poseStack.popPose();
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer, float x0, float y0, float x1, float y1, int color) {
        Matrix4f pose = poseStack.last().pose();
        consumer.addVertex(pose, x0, y0, 0).setColor(color).setUv(0, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y0, 0).setColor(color).setUv(1, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y1, 0).setColor(color).setUv(1, 0).setLight(LIGHT);
        consumer.addVertex(pose, x0, y1, 0).setColor(color).setUv(0, 0).setLight(LIGHT);
    }
}
