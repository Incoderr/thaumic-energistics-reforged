package thaumicenergistics.client.alchemy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import thaumicenergistics.alchemy.CrucibleAssemblerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.render.model.ObjModel;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.alchemy.CrucibleAssemblerBlockEntity;

public final class CrucibleAssemblerRenderer implements BlockEntityRenderer<CrucibleAssemblerBlockEntity> {
    private static final ResourceLocation MODEL = ThaumicEnergistics.id("models/obj/crucible_assembler.obj");
    private static final ResourceLocation FRAME = ThaumicEnergistics.id("textures/models/crucible.assembler.png");
    private static final String[][] SOLID_PARTS = {
        {"thaumatorium", "thaumcraft:textures/model/thaumatorium.png"},
        {"crucible1", "thaumcraft:textures/block/crucible1.png"},
        {"crucible2", "thaumcraft:textures/block/crucible2.png"},
        {"crucible3", "thaumcraft:textures/block/crucible3.png"},
    };

    public CrucibleAssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrucibleAssemblerBlockEntity assembler, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderAll(pose, buffers, light, assembler.getBlockState().getValue(CrucibleAssemblerBlock.FACING));
    }

    static void renderAll(PoseStack pose, MultiBufferSource buffers, int light, Direction facing) {
        ObjModel model = ObjModel.get(MODEL);
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(270.0F - facing.toYRot()));
        PoseStack.Pose last = pose.last();
        for (String[] part : SOLID_PARTS) {
            model.renderPart(part[0], last, buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(part[1]))), -1, light);
        }
        model.renderPart("frame", last, buffers.getBuffer(RenderType.entityCutoutNoCull(FRAME)), -1, light);
        model.renderPart("glass", last, buffers.getBuffer(RenderType.entityTranslucent(FRAME)), -1, light);
        pose.popPose();
    }

    public static final class Item extends BlockEntityWithoutLevelRenderer {
        public Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderAll(pose, buffers, light, Direction.NORTH);
        }
    }
}
