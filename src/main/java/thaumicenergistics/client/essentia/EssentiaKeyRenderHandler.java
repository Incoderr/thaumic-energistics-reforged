package thaumicenergistics.client.essentia;

import thaumicenergistics.client.platform.TeRenderTypes;
import appeng.client.api.AEKeyRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
import thaumicenergistics.essentia.EssentiaKey;

public final class EssentiaKeyRenderHandler implements AEKeyRenderer<EssentiaKey, EssentiaKeyRenderHandler.State> {
    private static final float HALF = 0.5F;

    public static final class State {
        @Nullable Aspect aspect;
    }

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y, EssentiaKey key) {
        AspectRenderer.drawTag(graphics, x, y, key.getAspect(), 0.0F, 0, 1.0F, false);
    }

    @Override
    public Class<State> stateClass() {
        return State.class;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public void extract(State state, EssentiaKey key, Level level, int seed) {
        state.aspect = key.getAspect();
    }

    @Override
    public void submit(PoseStack poseStack, State state, SubmitNodeCollector collector, int light) {
        Aspect aspect = state.aspect;
        if (aspect == null) {
            return;
        }
        int color = aspect.getColor();
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        collector.submitCustomGeometry(poseStack, TeRenderTypes.entityTranslucent(aspect.getImage()), (pose, buffer) -> {
            vertex(buffer, pose, -HALF, -HALF, 0.0F, 1.0F, red, green, blue, light);
            vertex(buffer, pose, HALF, -HALF, 1.0F, 1.0F, red, green, blue, light);
            vertex(buffer, pose, HALF, HALF, 1.0F, 0.0F, red, green, blue, light);
            vertex(buffer, pose, -HALF, HALF, 0.0F, 0.0F, red, green, blue, light);
        });
    }

    @Override
    public List<Component> getTooltip(EssentiaKey key) {
        return List.of(key.getDisplayName());
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
