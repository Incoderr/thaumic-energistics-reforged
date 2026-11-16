package thaumicenergistics.client.alchemy;

import thaumicenergistics.client.platform.TeRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.render.model.ObjModel;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.alchemy.CrucibleAssemblerBlock;
import thaumicenergistics.alchemy.CrucibleAssemblerBlockEntity;

public final class CrucibleAssemblerRenderer implements BlockEntityRenderer<CrucibleAssemblerBlockEntity, CrucibleAssemblerRenderer.State> {
    private static final Identifier MODEL = ThaumicEnergistics.id("models/obj/crucible_assembler.obj");
    private static final Identifier FRAME = ThaumicEnergistics.id("textures/models/crucible.assembler.png");
    private static final String[][] SOLID_PARTS = {
        {"thaumatorium", "thaumcraft:textures/model/thaumatorium.png"},
        {"crucible1", "thaumcraft:textures/block/crucible1.png"},
        {"crucible2", "thaumcraft:textures/block/crucible2.png"},
        {"crucible3", "thaumcraft:textures/block/crucible3.png"},
    };

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
    }

    public CrucibleAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CrucibleAssemblerBlockEntity assembler, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(assembler, state, partialTicks, camera, breakProgress);
        state.facing = assembler.getBlockState().getValue(CrucibleAssemblerBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        renderAll(collector, pose, state.lightCoords, state.facing);
    }

    public static void renderAll(SubmitNodeCollector collector, PoseStack pose, int light, Direction facing) {
        ObjModel model = ObjModel.get(MODEL);
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.rotateDegrees(Axis.YP, 270.0F - facing.toYRot());
        for (String[] part : SOLID_PARTS) {
            collector.submitCustomGeometry(pose, TeRenderTypes.entityCutout(Identifier.parse(part[1])), (last, buffer) -> model.renderPart(part[0], last, buffer, -1, light));
        }
        collector.submitCustomGeometry(pose, TeRenderTypes.entityCutout(FRAME), (last, buffer) -> model.renderPart("frame", last, buffer, -1, light));
        collector.submitCustomGeometry(pose, TeRenderTypes.entityTranslucent(FRAME), (last, buffer) -> model.renderPart("glass", last, buffer, -1, light));
        pose.popPose();
    }
}
