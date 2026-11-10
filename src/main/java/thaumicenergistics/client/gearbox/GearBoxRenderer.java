package thaumicenergistics.client.gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.gearbox.GearBoxBlockEntity;
import thaumicenergistics.gearbox.GearBoxFeature;

public final class GearBoxRenderer implements BlockEntityRenderer<GearBoxBlockEntity> {
    private static final ResourceLocation TEX_IRON = ThaumicEnergistics.id("textures/models/gearbox.iron.png");
    private static final ResourceLocation TEX_THAUMIUM = ThaumicEnergistics.id("textures/models/gearbox.thaumium.png");
    private static final float HALF_TURN = 1.570796325F;
    private static final float QUARTER_TURN = 0.7853981625F;
    private static final float EDGE_DISTANCE = 3.12F;
    private static final float SPOKE_OFFSET = 1.2F;
    private static final float[][] TRANSFORMS = new float[6][];

    static {
        TRANSFORMS[Direction.EAST.get3DDataValue()] = new float[] {0F, 0F, EDGE_DISTANCE, 0F, 0F};
        TRANSFORMS[Direction.WEST.get3DDataValue()] = new float[] {0F, 0F, -EDGE_DISTANCE, 0F, 0F};
        TRANSFORMS[Direction.UP.get3DDataValue()] = new float[] {0F, HALF_TURN, 0F, EDGE_DISTANCE, 0F};
        TRANSFORMS[Direction.DOWN.get3DDataValue()] = new float[] {0F, HALF_TURN, 0F, -EDGE_DISTANCE, 0F};
        TRANSFORMS[Direction.SOUTH.get3DDataValue()] = new float[] {HALF_TURN, 0F, 0F, 0F, EDGE_DISTANCE};
        TRANSFORMS[Direction.NORTH.get3DDataValue()] = new float[] {HALF_TURN, 0F, 0F, 0F, -EDGE_DISTANCE};
    }

    private static ModelPart transmission;
    private static ModelPart shaft;
    private static ModelPart spoke;

    public GearBoxRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static void build() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("transmission", CubeListBuilder.create().texOffs(1, 0).mirror().addBox(0F, 0F, 0F, 7, 7, 7), PartPose.offset(-3.5F, -3.5F, -3.5F));
        root.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(1, 15).mirror().addBox(-1.5F, -0.5F, -0.5F, 3, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("spoke", CubeListBuilder.create().texOffs(1, 18).mirror().addBox(-0.5F, -3.5F, -0.5F, 1, 7, 1), PartPose.ZERO);
        ModelPart baked = LayerDefinition.create(mesh, 32, 32).bakeRoot();
        transmission = baked.getChild("transmission");
        shaft = baked.getChild("shaft");
        spoke = baked.getChild("spoke");
    }

    private static void renderPart(ModelPart part, PoseStack pose, VertexConsumer buffer, int light, float offsetX, float offsetY, float offsetZ) {
        pose.pushPose();
        pose.translate(offsetX, offsetY, offsetZ);
        pose.scale(10.0F, 10.0F, 10.0F);
        part.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    public static void render(boolean thaumium, boolean[] facingCrankable, float rotation, PoseStack pose, MultiBufferSource buffers, int light) {
        if (transmission == null) {
            build();
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(thaumium ? TEX_THAUMIUM : TEX_IRON));
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(0.12F, 0.12F, 0.12F);
        renderPart(transmission, pose, buffer, light, 0, 0, 0);
        for (int side = 0; side < 6; side++) {
            float[] transform = TRANSFORMS[side];
            shaft.xRot = rotation;
            shaft.yRot = transform[0];
            shaft.zRot = transform[1];
            renderPart(shaft, pose, buffer, light, transform[2], transform[3], transform[4]);
            if (!facingCrankable[side]) {
                spoke.yRot = transform[0];
                spoke.zRot = transform[1];
                for (int step = 1; step <= 4; step++) {
                    spoke.xRot = rotation + QUARTER_TURN * step;
                    renderPart(spoke, pose, buffer, light, transform[2] * SPOKE_OFFSET, transform[3] * SPOKE_OFFSET, transform[4] * SPOKE_OFFSET);
                }
            }
        }
        pose.popPose();
    }

    @Override
    public void render(GearBoxBlockEntity gearbox, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        boolean[] facing = new boolean[6];
        for (Direction side : Direction.values()) {
            facing[side.get3DDataValue()] = gearbox.isSideFacingCrankable(side);
        }
        render(gearbox.isThaumium(), facing, gearbox.shaftRotation(), pose, buffers, light);
    }

    public static final class Item extends BlockEntityWithoutLevelRenderer {
        public Item() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            render(stack.is(GearBoxFeature.THAUMIUM_GEAR_BOX.asItem()), new boolean[6], 0.0F, pose, buffers, light);
        }
    }
}
