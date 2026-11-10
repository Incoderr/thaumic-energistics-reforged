package thaumicenergistics.client.infusion;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.render.RunicMatrixRendering;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.assembler.ArcaneAssemblerModel;
import thaumicenergistics.infusion.InfusionAssemblerBlockEntity;

public final class InfusionAssemblerRenderer implements BlockEntityRenderer<InfusionAssemblerBlockEntity> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/models/infusion.assembler.png");
    private static final float MATRIX_SCALE = 0.5F;

    public InfusionAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(InfusionAssemblerBlockEntity assembler, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float ticks = Minecraft.getInstance().player == null ? 0.0F : Minecraft.getInstance().player.tickCount + partialTick;
        renderAll(pose, buffers, light, assembler.isCrafting(), ticks);
    }

    static void renderAll(PoseStack pose, MultiBufferSource buffers, int light, boolean crafting, float ticks) {
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(MATRIX_SCALE, MATRIX_SCALE, MATRIX_SCALE);
        pose.translate(-0.5F, -0.5F, -0.5F);
        RunicMatrixRendering.submit(buffers, pose, light, true, crafting, crafting, 1.0F, 0, crafting ? 50 : 0, ticks);
        pose.popPose();
        ArcaneAssemblerModel.render(pose, buffers, light, TEXTURE);
    }

    public static final class Item extends BlockEntityWithoutLevelRenderer {
        public Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderAll(pose, buffers, light, false, 0.0F);
        }
    }
}
