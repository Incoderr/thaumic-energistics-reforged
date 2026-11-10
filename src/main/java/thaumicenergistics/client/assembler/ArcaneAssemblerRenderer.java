package thaumicenergistics.client.assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumicenergistics.assembler.ArcaneAssemblerBlockEntity;

public final class ArcaneAssemblerRenderer implements BlockEntityRenderer<ArcaneAssemblerBlockEntity> {
    public ArcaneAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ArcaneAssemblerBlockEntity assembler, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ArcaneAssemblerModel.render(pose, buffers, light);
    }

    public static final class Item extends BlockEntityWithoutLevelRenderer {
        public Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            ArcaneAssemblerModel.render(pose, buffers, light);
        }
    }
}
