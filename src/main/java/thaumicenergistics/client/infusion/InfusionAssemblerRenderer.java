package thaumicenergistics.client.infusion;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.render.RunicMatrixRendering;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.assembler.ArcaneAssemblerModel;
import thaumicenergistics.infusion.InfusionAssemblerBlockEntity;

public final class InfusionAssemblerRenderer implements BlockEntityRenderer<InfusionAssemblerBlockEntity, InfusionAssemblerRenderer.State> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/models/infusion.assembler.png");
    private static final float MATRIX_SCALE = 0.5F;

    public static class State extends BlockEntityRenderState {
        boolean crafting;
        float ticks;
    }

    public InfusionAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(InfusionAssemblerBlockEntity assembler, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(assembler, state, partialTicks, camera, breakProgress);
        state.crafting = assembler.isCrafting();
        state.ticks = Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        renderAll(collector, pose, state.lightCoords, state.crafting, state.ticks);
    }

    public static void renderAll(SubmitNodeCollector collector, PoseStack pose, int light, boolean crafting, float ticks) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(MATRIX_SCALE, MATRIX_SCALE, MATRIX_SCALE);
        pose.translate(-0.5F, -0.5F, -0.5F);
        RunicMatrixRendering.submit(collector, pose, light, true, crafting, crafting, 1.0F, 0, crafting ? 50 : 0, ticks);
        pose.popPose();
        ArcaneAssemblerModel.render(collector, pose, light, TEXTURE);
    }
}
