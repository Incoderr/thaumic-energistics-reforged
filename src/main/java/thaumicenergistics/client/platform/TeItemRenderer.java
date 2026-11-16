package thaumicenergistics.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record TeItemRenderer(Drawer drawer) implements NoDataSpecialModelRenderer {
    public interface Drawer {
        void draw(SubmitNodeCollector collector, PoseStack poseStack, int light);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        drawer.draw(collector, poseStack, lightCoords);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (float x : new float[] {0.0F, 1.0F}) {
            for (float y : new float[] {0.0F, 1.0F}) {
                for (float z : new float[] {0.0F, 1.0F}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    public static MapCodec<Unbaked> codec(Drawer drawer) {
        Unbaked[] holder = new Unbaked[1];
        MapCodec<Unbaked> codec = MapCodec.unit(() -> holder[0]);
        holder[0] = new Unbaked(drawer, codec);
        return codec;
    }

    public record Unbaked(Drawer drawer, MapCodec<Unbaked> codec) implements NoDataSpecialModelRenderer.Unbaked {
        @Override
        public TeItemRenderer bake(BakingContext context) {
            return new TeItemRenderer(drawer);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return codec;
        }
    }
}
