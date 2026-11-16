package thaumicenergistics.client.monitor;

import thaumicenergistics.client.platform.TeRenderTypes;
import appeng.api.orientation.BlockOrientation;
import appeng.client.api.renderer.parts.PartDynamicRenderState;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.render.BlockEntityRenderHelper;
import appeng.util.ReadableNumberConverter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;

public final class EssentiaMonitorRenderer implements PartRenderer<EssentiaStorageMonitorPart, EssentiaMonitorRenderer.State> {
    private static final Identifier LOCK_TEXTURE = ThaumicEnergistics.id("textures/block/parts/monitor.locked.png");
    private static final int LIGHT = LightCoordsUtil.pack(13, 13);
    private static final float DEPTH = 0.505F;
    private static final float ICON_SIZE = 0.38F;
    private static final float TEXT_SCALE = 1.0F / 62.0F;

    public static class State extends PartDynamicRenderState {
        @Nullable Aspect aspect;
        boolean locked;
        String amount = "";
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
    public void extract(EssentiaStorageMonitorPart part, State state, float partialTicks) {
        state.side = part.getSide();
        state.aspect = part.isActive() ? part.getTrackedAspect() : null;
        state.locked = part.isLocked();
        state.amount = ReadableNumberConverter.format(part.getTrackedAmount(), 4);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Aspect aspect = state.aspect;
        if (aspect == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        BlockEntityRenderHelper.rotateToFace(poseStack, BlockOrientation.get(state.side, 0));
        poseStack.translate(0, 0, DEPTH);
        int color = aspect.getColor();
        int tint = ARGB.color(229, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
        collector.submitCustomGeometry(poseStack, TeRenderTypes.text(aspect.getImage()), (pose, buffer) -> quad(pose.pose(), buffer, -0.2F, 0.25F - ICON_SIZE, -0.2F + ICON_SIZE, 0.25F, 0, 1, tint));
        float lockU = state.locked ? 0.0F : 0.5F;
        poseStack.translate(0, 0, 0.003);
        collector.submitCustomGeometry(poseStack, TeRenderTypes.text(LOCK_TEXTURE), (pose, buffer) -> quad(pose.pose(), buffer, 0.2F, 0.21F, 0.3F, 0.31F, lockU, lockU + 0.5F, 0x80FFFFFF));
        Font font = Minecraft.getInstance().font;
        poseStack.translate(0, -0.15, 0.003);
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        collector.submitText(poseStack, -font.width(state.amount) / 2.0F, 0, Component.literal(state.amount).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET, LIGHT, 0xFF000000, 0, 0);
        poseStack.popPose();
    }

    private static void quad(Matrix4f pose, VertexConsumer consumer, float x0, float y0, float x1, float y1, float u0, float u1, int color) {
        consumer.addVertex(pose, x0, y0, 0).setColor(color).setUv(u0, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y0, 0).setColor(color).setUv(u1, 1).setLight(LIGHT);
        consumer.addVertex(pose, x1, y1, 0).setColor(color).setUv(u1, 0).setLight(LIGHT);
        consumer.addVertex(pose, x0, y1, 0).setColor(color).setUv(u0, 0).setLight(LIGHT);
    }
}
