package thaumicenergistics.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.AspectList;

public record ArcanePattern(List<ItemStack> grid, ItemStack result, AspectList aspects) {
    public static final int GRID_SIZE = 9;
    public static final Codec<ArcanePattern> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("grid").forGetter(ArcanePattern::grid),
        ItemStack.CODEC.fieldOf("result").forGetter(ArcanePattern::result),
        AspectList.CODEC.fieldOf("aspects").forGetter(ArcanePattern::aspects)
    ).apply(instance, ArcanePattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcanePattern> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), ArcanePattern::grid,
        ItemStack.STREAM_CODEC, ArcanePattern::result,
        AspectList.STREAM_CODEC.cast(), ArcanePattern::aspects,
        ArcanePattern::new
    );

    public ArcanePattern {
        List<ItemStack> copy = new ArrayList<>(GRID_SIZE);
        for (int index = 0; index < GRID_SIZE; index++) {
            copy.add(index < grid.size() ? grid.get(index).copyWithCount(Math.min(1, grid.get(index).getCount())) : ItemStack.EMPTY);
        }
        grid = List.copyOf(copy);
        result = result.copy();
        aspects = aspects.copy();
    }

    public boolean isValid() {
        return !result.isEmpty();
    }

    public boolean producesSame(ItemStack stack) {
        return ItemStack.matches(result, stack);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArcanePattern pattern && ItemStack.matches(result, pattern.result) && aspects.equals(pattern.aspects)
            && ItemStack.listMatches(grid, pattern.grid);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(result);
    }
}
