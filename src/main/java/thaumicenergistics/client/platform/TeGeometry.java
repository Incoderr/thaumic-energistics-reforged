package thaumicenergistics.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class TeGeometry {
    private TeGeometry() {
    }

    public static void submit(OrderedSubmitNodeCollector collector, PoseStack poseStack, RenderType type, BiConsumer<PoseStack, VertexConsumer> body) {
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            body.accept(local, buffer);
        });
    }
}
