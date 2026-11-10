package thaumicenergistics.client.golem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import thaumcraft.entity.golem.GolemBase;
import thaumicenergistics.golem.BackpackData;
import thaumicenergistics.golem.BackpackSkin;
import thaumicenergistics.golem.GolemBackpackFeature;

public final class GolemBackpackRenderer {
    private static final float FLIP = 3.14F;
    private static final float TEXTURE_SIZE = 16.0F;
    private static final float PEARL_MIN_U = 8.0F / TEXTURE_SIZE;
    private static final float PEARL_MAX_U = 13.5F / TEXTURE_SIZE;
    private static final float PEARL_MIN_V = 6.0F / TEXTURE_SIZE;
    private static final float PEARL_MAX_V = 11.5F / TEXTURE_SIZE;
    private static final float PEARL_SCALE = 0.2F;
    private static final float ANTENNA_WIDTH = 0.32F;
    private static final float PEARL_FACE_DISTANCE = -0.013F / PEARL_SCALE;
    private static final float PEARL_2FD = PEARL_FACE_DISTANCE + PEARL_FACE_DISTANCE;
    private static final Map<GolemBase, float[]> PEARL_ROTATION = new WeakHashMap<>();

    private static ModelPart antenna;
    private static ModelPart packBack;
    private static ModelPart packFront;

    private GolemBackpackRenderer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GolemBackpackRenderer::onRender);
    }

    private static void build() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("antenna", CubeListBuilder.create().texOffs(10, 0).mirror().addBox(-0.5F, -6F, -0.5F, 1, 3, 1), PartPose.rotation(FLIP, 0, 0));
        root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1F, -3F, -3F, 2, 6, 6), PartPose.rotation(FLIP, 0, 0));
        root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(2, 0).mirror().addBox(-1.5F, -1F, -2F, 1, 2, 4), PartPose.rotation(FLIP, 0, 0));
        ModelPart baked = LayerDefinition.create(mesh, 16, 16).bakeRoot();
        antenna = baked.getChild("antenna");
        packBack = baked.getChild("back");
        packFront = baked.getChild("front");
    }

    private static void onRender(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof GolemBase golem)) {
            return;
        }
        BackpackData.Client state = GolemBackpackFeature.CLIENT.get(golem);
        if (state == null || !state.hasBackpack()) {
            return;
        }
        if (antenna == null) {
            build();
        }
        float partialTick = event.getPartialTick();
        int color;
        if (golem.hurtTime > 0) {
            color = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);
        } else if (golem.healing > 0) {
            color = FastColor.ARGB32.colorFromFloat(1.0F, 0.5F, 1.0F, 0.5F);
        } else {
            color = -1;
        }
        boolean active = state.inRange() && !golem.inactive;
        float[] rotation = PEARL_ROTATION.computeIfAbsent(golem, key -> new float[1]);
        if (active && !Minecraft.getInstance().isPaused()) {
            rotation[0] += partialTick * 2.0F;
            if (rotation[0] >= 360.0F) {
                rotation[0] -= 360.0F;
            }
        }
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0F, 0.64F, 0.0F);
        float yaw = 270.0F - (golem.yBodyRot * partialTick + golem.yBodyRotO * (1.0F - partialTick));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.16F, 0.0F, 0.0F);
        pose.scale(0.8F, 0.8F, 0.8F);
        BackpackSkin skin = BackpackSkin.byId(state.skin());
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(skin.texture()));
        int light = event.getPackedLight();
        packBack.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        packFront.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        antenna.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        int pearlColor = state.inRange() ? color : FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F);
        renderPearl(pose, buffer, light, Mth.wrapDegrees(rotation[0]), pearlColor);
        pose.popPose();
    }

    private static void renderPearl(PoseStack pose, VertexConsumer buffer, int light, float rotation, int color) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.0955F, 0.275F, -0.045F);
        pose.scale(PEARL_SCALE, PEARL_SCALE, PEARL_SCALE);
        faces(pose, buffer, light, color, PEARL_MIN_U, PEARL_MAX_U);
        pose.translate(0.0F, 0.0F, ANTENNA_WIDTH - PEARL_2FD);
        faces(pose, buffer, light, color, PEARL_MAX_U, PEARL_MIN_U);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(-ANTENNA_WIDTH - PEARL_FACE_DISTANCE, 0.0F, ANTENNA_WIDTH + PEARL_FACE_DISTANCE);
        faces(pose, buffer, light, color, PEARL_MAX_U, PEARL_MIN_U);
        pose.translate(0.0F, 0.0F, ANTENNA_WIDTH - PEARL_2FD);
        faces(pose, buffer, light, color, PEARL_MIN_U, PEARL_MAX_U);
        pose.popPose();
    }

    private static void faces(PoseStack pose, VertexConsumer buffer, int light, int color, float minU, float maxU) {
        Matrix4f matrix = pose.last().pose();
        PoseStack.Pose last = pose.last();
        vertex(buffer, matrix, last, 0, 0, maxU, PEARL_MAX_V, light, color, 1);
        vertex(buffer, matrix, last, 1, 0, minU, PEARL_MAX_V, light, color, 1);
        vertex(buffer, matrix, last, 1, 1, minU, PEARL_MIN_V, light, color, 1);
        vertex(buffer, matrix, last, 0, 1, maxU, PEARL_MIN_V, light, color, 1);
        vertex(buffer, matrix, last, 0, 1, maxU, PEARL_MIN_V, light, color, -1);
        vertex(buffer, matrix, last, 1, 1, minU, PEARL_MIN_V, light, color, -1);
        vertex(buffer, matrix, last, 1, 0, minU, PEARL_MAX_V, light, color, -1);
        vertex(buffer, matrix, last, 0, 0, maxU, PEARL_MAX_V, light, color, -1);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, PoseStack.Pose last, float x, float y, float u, float v, int light, int color, float normalZ) {
        buffer.addVertex(matrix, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0.0F, 0.0F, normalZ);
    }
}
