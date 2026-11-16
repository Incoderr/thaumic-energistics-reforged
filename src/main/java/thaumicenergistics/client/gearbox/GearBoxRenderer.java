package thaumicenergistics.client.gearbox;

import thaumicenergistics.client.platform.TeRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.platform.TeGeometry;
import thaumicenergistics.gearbox.GearBoxBlockEntity;

public final class GearBoxRenderer implements BlockEntityRenderer<GearBoxBlockEntity, GearBoxRenderer.State> {
    private static final Identifier TEX_IRON = ThaumicEnergistics.id("textures/models/gearbox.iron.png");
    private static final Identifier TEX_THAUMIUM = ThaumicEnergistics.id("textures/models/gearbox.thaumium.png");
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

    public static class State extends BlockEntityRenderState {
        boolean thaumium;
        final boolean[] facingCrankable = new boolean[6];
        float rotation;
    }

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

    public static void render(SubmitNodeCollector collector, boolean thaumium, boolean[] facingCrankable, float rotation, PoseStack poseStack, int light) {
        if (transmission == null) {
            build();
        }
        boolean[] facing = facingCrankable.clone();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.12F, 0.12F, 0.12F);
        TeGeometry.submit(collector, poseStack, TeRenderTypes.entityCutout(thaumium ? TEX_THAUMIUM : TEX_IRON), (pose, buffer) -> {
            renderPart(transmission, pose, buffer, light, 0, 0, 0);
            for (int side = 0; side < 6; side++) {
                float[] transform = TRANSFORMS[side];
                shaft.xRot = rotation;
                shaft.yRot = transform[0];
                shaft.zRot = transform[1];
                renderPart(shaft, pose, buffer, light, transform[2], transform[3], transform[4]);
                if (!facing[side]) {
                    spoke.yRot = transform[0];
                    spoke.zRot = transform[1];
                    for (int step = 1; step <= 4; step++) {
                        spoke.xRot = rotation + QUARTER_TURN * step;
                        renderPart(spoke, pose, buffer, light, transform[2] * SPOKE_OFFSET, transform[3] * SPOKE_OFFSET, transform[4] * SPOKE_OFFSET);
                    }
                }
            }
        });
        poseStack.popPose();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GearBoxBlockEntity gearbox, State state, float partialTicks, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(gearbox, state, partialTicks, camera, breakProgress);
        state.thaumium = gearbox.isThaumium();
        state.rotation = gearbox.shaftRotation();
        for (Direction side : Direction.values()) {
            state.facingCrankable[side.get3DDataValue()] = gearbox.isSideFacingCrankable(side);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        render(collector, state.thaumium, state.facingCrankable, state.rotation, pose, state.lightCoords);
    }
}
