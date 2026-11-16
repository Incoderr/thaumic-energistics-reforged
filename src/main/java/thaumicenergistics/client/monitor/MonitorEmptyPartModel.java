package thaumicenergistics.client.monitor;

import appeng.client.api.model.parts.PartModel;
import appeng.parts.automation.PartModelData;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.model.data.ModelData;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;

public record MonitorEmptyPartModel(BlockStateModelPart on, BlockStateModelPart off) implements PartModel {
    public static final Identifier ID = ThaumicEnergistics.id("monitor_empty");

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, ModelData data, RandomSource random, List<BlockStateModelPart> parts) {
        if (Boolean.TRUE.equals(data.get(EssentiaStorageMonitorPart.EMPTY))) {
            parts.add(data.get(PartModelData.STATUS_INDICATOR) == PartModelData.StatusIndicatorState.ACTIVE ? on : off);
        }
    }

    @Override
    public Material.Baked particleMaterial() {
        return off.particleMaterial();
    }

    public record Unbaked(Identifier on, Identifier off) implements PartModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.fieldOf("on").forGetter(Unbaked::on),
                Identifier.CODEC.fieldOf("off").forGetter(Unbaked::off)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<? extends PartModel.Unbaked> codec() {
            return MAP_CODEC;
        }

        @Override
        public PartModel bake(ModelBaker baker, ModelState state) {
            return new MonitorEmptyPartModel(SimpleModelWrapper.bake(baker, on, state), SimpleModelWrapper.bake(baker, off, state));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(on);
            resolver.markDependency(off);
        }
    }
}
