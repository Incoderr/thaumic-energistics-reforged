package thaumicenergistics.client.essentia;

import appeng.api.client.AEKeyRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
import thaumicenergistics.essentia.EssentiaKey;

public final class EssentiaKeyRenderHandler implements AEKeyRenderHandler<EssentiaKey> {
    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphics graphics, int x, int y, EssentiaKey key) {
        AspectRenderer.drawTag(graphics, x, y, key.getAspect(), 0.0F, 0, 1.0F, false);
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, EssentiaKey key, float scale, int light, Level level) {
        Aspect aspect = key.getAspect();
        int color = aspect.getColor();
        int red = FastColor.ARGB32.red(color);
        int green = FastColor.ARGB32.green(color);
        int blue = FastColor.ARGB32.blue(color);
        float half = scale / 2.0F;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
        vertex(buffer, pose, -half, -half, 0.0F, 1.0F, red, green, blue, light);
        vertex(buffer, pose, half, -half, 1.0F, 1.0F, red, green, blue, light);
        vertex(buffer, pose, half, half, 1.0F, 0.0F, red, green, blue, light);
        vertex(buffer, pose, -half, half, 0.0F, 0.0F, red, green, blue, light);
    }

    @Override
    public Component getDisplayName(EssentiaKey key) {
        return key.getDisplayName();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int red, int green, int blue, int light) {
        buffer.addVertex(pose, x, y, 0.0F)
            .setColor(red, green, blue, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
