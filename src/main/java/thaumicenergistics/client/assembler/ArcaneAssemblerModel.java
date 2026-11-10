package thaumicenergistics.client.assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;

public final class ArcaneAssemblerModel {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/models/arcane.assembler.png");
    private static final float HALF_TURN = 1.570796325F;
    private static final float FULL_TURN = HALF_TURN + HALF_TURN;
    private static final float SCALE = 0.047F * 0.625F * 16.0F;

    private static ModelPart root;
    private static List<ModelPart> frames;
    private static List<ModelPart> glass;

    private ArcaneAssemblerModel() {
    }

    private static int counter;

    private static String add(PartDefinition parent, int u, int v, float x, float y, float z, int dx, int dy, int dz, float rx, float ry, float rz) {
        String name = "part" + counter++;
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, dx, dy, dz), PartPose.rotation(rx, ry, rz));
        return name;
    }

    private static void build() {
        counter = 0;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        List<String> frameNames = new ArrayList<>();
        List<String> glassNames = new ArrayList<>();
        for (float rotation : new float[] {0.0F, HALF_TURN, -HALF_TURN, FULL_TURN}) {
            frameNames.add(add(parts, 0, 0, -17F, -17F, -15F, 34, 2, 2, 0.0F, rotation, 0.0F));
            frameNames.add(add(parts, 0, 0, -17F, 15F, -15F, 34, 2, 2, 0.0F, rotation, 0.0F));
            frameNames.add(add(parts, 0, 0, -17F, -17F, -15F, 34, 2, 2, -rotation, 0.0F, -1.570796F));
            frameNames.add(add(parts, 0, 0, -17F, -17F, -15F, 34, 2, 2, rotation, 0.0F, 1.570796F));
        }
        float[][] sides = {{0.0F, 0.0F}, {HALF_TURN, 0.0F}, {-HALF_TURN, 0.0F}, {FULL_TURN, 0.0F}, {0.0F, HALF_TURN}, {0.0F, -HALF_TURN}};
        for (float[] side : sides) {
            frameNames.add(add(parts, 0, 4, -15F, -15F, -15F, 30, 2, 2, side[1], side[0], 0.0F));
            frameNames.add(add(parts, 0, 4, -15F, 13F, -15F, 30, 2, 2, side[1], side[0], 0.0F));
            frameNames.add(add(parts, 0, 4, -15F, -15F, -15F, 30, 2, 2, side[1], side[0], -1.570796F));
            frameNames.add(add(parts, 0, 4, -15F, -15F, -15F, 30, 2, 2, side[1], side[0], 1.570796F));
        }
        for (float[] side : sides) {
            glassNames.add(add(parts, 0, 37, -13F, -13F, -14F, 26, 26, 1, side[1], side[0], 0.0F));
        }
        root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        frames = frameNames.stream().map(root::getChild).toList();
        glass = glassNames.stream().map(root::getChild).toList();
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, int light) {
        if (root == null) {
            build();
        }
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(SCALE, SCALE, SCALE);
        VertexConsumer solid = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        for (ModelPart part : frames) {
            part.render(pose, solid, light, OverlayTexture.NO_OVERLAY);
        }
        VertexConsumer translucent = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (ModelPart part : glass) {
            part.render(pose, translucent, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }
}
