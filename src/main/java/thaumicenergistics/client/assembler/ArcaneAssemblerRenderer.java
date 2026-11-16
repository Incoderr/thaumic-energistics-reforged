package thaumicenergistics.client.assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import thaumicenergistics.assembler.ArcaneAssemblerBlockEntity;

public final class ArcaneAssemblerRenderer implements BlockEntityRenderer<ArcaneAssemblerBlockEntity, BlockEntityRenderState> {
    public ArcaneAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        ArcaneAssemblerModel.render(collector, pose, state.lightCoords);
    }

    public static void renderItem(SubmitNodeCollector collector, PoseStack pose, int light) {
        ArcaneAssemblerModel.render(collector, pose, light);
    }
}
