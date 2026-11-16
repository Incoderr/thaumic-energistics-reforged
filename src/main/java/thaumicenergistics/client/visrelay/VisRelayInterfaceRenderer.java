package thaumicenergistics.client.visrelay;

import thaumicenergistics.client.platform.TeRenderTypes;
import appeng.api.orientation.BlockOrientation;
import appeng.client.api.renderer.parts.PartDynamicRenderState;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.render.BlockEntityRenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.visrelay.VisRelayInterfacePart;

public final class VisRelayInterfaceRenderer implements PartRenderer<VisRelayInterfacePart, VisRelayInterfaceRenderer.State> {
    private static final Identifier RUNES_TEXTURE = ThaumicEnergistics.id("textures/block/parts/vis.interface.runes.png");
    private static final int LIGHT = LightCoordsUtil.pack(13, 13);
    private static final float DEPTH = 0.4385F;
    private static final float HALF_SIZE = 0.25F;

    public static class State extends PartDynamicRenderState {
        int color;
    }

    @Override
    public State createState() {
        return new State();
    }

    @Override
    public Class<State> stateClass() {
        return State.class;
    }

    @Override
    public void extract(VisRelayInterfacePart part, State state, float partialTicks) {
        state.side = part.getSide();
        state.color = part.getDrainColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.color == 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        BlockEntityRenderHelper.rotateToFace(poseStack, BlockOrientation.get(state.side, 0));
        poseStack.translate(0, 0, DEPTH);
        int color = 0xFF000000 | (state.color & 0xFFFFFF);
        collector.submitCustomGeometry(poseStack, TeRenderTypes.text(RUNES_TEXTURE), (pose, buffer) -> quad(pose.pose(), buffer, -HALF_SIZE, -HALF_SIZE, HALF_SIZE, HALF_SIZE, color));
        poseStack.popPose();
    }

    private static void quad(Matrix4f pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, int color) {
        consumer.addVertex(pose, x0, y0, 0).setColor(color).setUv(0, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y0, 0).setColor(color).setUv(1, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y1, 0).setColor(color).setUv(1, 0).setLight(LIGHT);
        consumer.addVertex(pose, x0, y1, 0).setColor(color).setUv(0, 0).setLight(LIGHT);
    }
}
