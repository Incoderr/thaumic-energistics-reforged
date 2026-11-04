package thaumicenergistics.provider;

import appeng.blockentity.AEBaseBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeNetwork;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.research.TeResearch;

public final class ProviderFeature {
    public static final String ESSENTIA_PROVIDER_RECIPE = TeRecipes.key("BLOCK_ESSENTIA_PROVIDER");
    public static final String INFUSION_PROVIDER_RECIPE = TeRecipes.key("BLOCK_INFUSION_PROVIDER");

    public static final DeferredBlock<EssentiaProviderBlock> ESSENTIA_PROVIDER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "essentia_provider", EssentiaProviderBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredBlock<InfusionProviderBlock> INFUSION_PROVIDER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "infusion_provider", InfusionProviderBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaProviderBlockEntity>> ESSENTIA_PROVIDER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "essentia_provider", () -> BlockEntityType.Builder.of(EssentiaProviderBlockEntity::new, ESSENTIA_PROVIDER.get()).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionProviderBlockEntity>> INFUSION_PROVIDER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "infusion_provider", () -> BlockEntityType.Builder.of(InfusionProviderBlockEntity::new, INFUSION_PROVIDER.get()).build(null)
    );

    private ProviderFeature() {
    }

    public static void load() {
    }

    public static void setup() {
        AEBaseBlockEntity.registerBlockEntityItem(ESSENTIA_PROVIDER_ENTITY.get(), ESSENTIA_PROVIDER.asItem());
        AEBaseBlockEntity.registerBlockEntityItem(INFUSION_PROVIDER_ENTITY.get(), INFUSION_PROVIDER.asItem());
    }

    public static void payloads() {
        TeNetwork.toClient(ProviderRunesPayload.TYPE, ProviderRunesPayload.STREAM_CODEC);
    }

    public static final class EssentiaProviderBlock extends ProviderBlock {
        public EssentiaProviderBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new EssentiaProviderBlockEntity(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide() || type != ESSENTIA_PROVIDER_ENTITY.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<EssentiaProviderBlockEntity>) EssentiaProviderBlockEntity::serverTick;
        }
    }

    public static final class InfusionProviderBlock extends ProviderBlock {
        public InfusionProviderBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new InfusionProviderBlockEntity(pos, state);
        }
    }

    public static void recipes() {
        TcIngredient diffusion = TcIngredient.item(CoreFeature.DIFFUSION_CORE.getId().toString());
        TcIngredient coalescence = TcIngredient.item(CoreFeature.COALESCENCE_CORE.getId().toString());
        TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
        TcIngredient filterTube = TcIngredient.item("thaumcraft:filtered_essentia_tube");
        TcIngredient waterShard = TcIngredient.item("thaumcraft:water_shard");
        TcIngredient airShard = TcIngredient.item("thaumcraft:air_shard");
        TcIngredient mirror = TcIngredient.item("thaumcraft:essentia_mirror");
        AspectList aspects = new AspectList().add(Aspect.MECHANISM, 64).add(Aspect.MAGIC, 32).add(Aspect.ORDER, 32).add(Aspect.EXCHANGE, 16);
        TeRecipes.register(new InfusionRecipe(
            ESSENTIA_PROVIDER_RECIPE, TeResearch.ESSENTIA_PROVIDER, TcStack.of(ESSENTIA_PROVIDER.getId().toString()), 3, aspects,
            TcIngredient.item("ae2:interface"),
            List.of(filterTube, salis, coalescence, waterShard, filterTube, salis, diffusion, waterShard)
        ));
        TeRecipes.register(new InfusionRecipe(
            INFUSION_PROVIDER_RECIPE, TeResearch.INFUSION_PROVIDER, TcStack.of(INFUSION_PROVIDER.getId().toString()), 4, aspects.copy(),
            TcIngredient.item("ae2:interface"),
            List.of(mirror, salis, coalescence, airShard, mirror, salis, coalescence, airShard)
        ));
    }

    public static void research() {
        TeResearch.item(
                TeResearch.ESSENTIA_PROVIDER,
                new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.MAGIC, 5).add(Aspect.ORDER, 3).add(Aspect.SENSES, 7),
                -2, -4, TeResearch.COMPLEXITY_LARGE, TcStack.of(ESSENTIA_PROVIDER.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.ESSENTIA_PROVIDER, 1), ResearchPage.infusion(ESSENTIA_PROVIDER_RECIPE))
            .setParents(TeResearch.IO)
            .setParentsHidden(TeResearch.INFUSION, TeResearch.TUBEFILTER)
            .setConcealed()
            .registerResearchItem();
        TeResearch.item(
                TeResearch.INFUSION_PROVIDER,
                new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.MAGIC, 3).add(Aspect.EXCHANGE, 7).add(Aspect.MOTION, 7).add(Aspect.SENSES, 5),
                -5, -2, TeResearch.COMPLEXITY_LARGE, TcStack.of(INFUSION_PROVIDER.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.INFUSION_PROVIDER, 1), ResearchPage.infusion(INFUSION_PROVIDER_RECIPE))
            .setParents(TeResearch.IO, TeResearch.pseudo(TeResearch.MIRROR), TeResearch.pseudo(TeResearch.INFUSION))
            .setParentsHidden(TeResearch.MIRROR, TeResearch.INFUSION)
            .setConcealed()
            .setSpecial()
            .registerResearchItem();
    }
}
