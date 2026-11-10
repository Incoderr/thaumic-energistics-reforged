package thaumicenergistics.alchemy;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeCapabilities;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class AlchemyFeature {
    public static final String CORE_RECIPE = TeRecipes.key("ITEM_ALCHEMY_CORE");
    public static final String INSCRIBER_RECIPE = TeRecipes.key("BLOCK_ALCHEMY_INSCRIBER");
    public static final String ASSEMBLER_RECIPE = TeRecipes.key("BLOCK_CRUCIBLE_ASSEMBLER");

    public static final TeStackKey<List<CruciblePattern>> PATTERNS = TeStackKey.register(
        "alchemy_core_patterns", CruciblePattern.CODEC.listOf(), CruciblePattern.STREAM_CODEC.apply(ByteBufCodecs.list())
    );
    public static final TeStackKey<CruciblePattern> PATTERN = TeStackKey.register(
        "crucible_assembler_pattern", CruciblePattern.CODEC, CruciblePattern.STREAM_CODEC
    );
    public static final DeferredItem<AlchemyCore> CORE = ModItems.ITEMS.registerItem("alchemy_core", AlchemyCore::new, properties -> properties);
    public static final DeferredBlock<AlchemyInscriberBlock> INSCRIBER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "alchemy_inscriber", AlchemyInscriberBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemyInscriberBlockEntity>> INSCRIBER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "alchemy_inscriber", () -> BlockEntityType.Builder.of(AlchemyInscriberBlockEntity::new, INSCRIBER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<AlchemyInscriberMenu>> INSCRIBER_MENU = ModMenus.MENUS.register(
        "alchemy_inscriber", () -> Menus.withPos(AlchemyInscriberMenu::new)
    );
    public static final DeferredBlock<CrucibleAssemblerBlock> ASSEMBLER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "crucible_assembler", CrucibleAssemblerBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL).noOcclusion()
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrucibleAssemblerBlockEntity>> ASSEMBLER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "crucible_assembler", () -> BlockEntityType.Builder.of(CrucibleAssemblerBlockEntity::new, ASSEMBLER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<CrucibleAssemblerMenu>> ASSEMBLER_MENU = ModMenus.MENUS.register(
        "crucible_assembler", () -> Menus.withPos(CrucibleAssemblerMenu::new)
    );

    private AlchemyFeature() {
    }

    public static void load() {
        TeCapabilities.gridHost(ASSEMBLER_ENTITY);
    }

    public static void setup() {
        AEBaseBlockEntity.registerBlockEntityItem(ASSEMBLER_ENTITY.get(), ASSEMBLER.asItem());
        Upgrades.add(AEItems.SPEED_CARD, ASSEMBLER.get(), CrucibleAssemblerBlockEntity.MAX_SPEED_UPGRADES);
        PatternDetailsHelper.registerDecoder(new CrucibleAssemblerPattern.Decoder());
    }

    public static void recipes() {
        TeRecipes.register(new InfusionRecipe(
            ASSEMBLER_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(ASSEMBLER.getId().toString()), 6,
            new AspectList().add(Aspect.CRAFT, 64).add(Aspect.EXCHANGE, 48).add(Aspect.WATER, 32).add(Aspect.FIRE, 32).add(Aspect.MECHANISM, 32).add(Aspect.MAGIC, 24),
            TcIngredient.item("thaumicenergistics:arcane_assembler"),
            List.of(
                TcIngredient.item("thaumcraft:advanced_alchemical_construct"), TcIngredient.item("thaumcraft:crucible"),
                TcIngredient.item("thaumcraft:alchemical_centrifuge"), TcIngredient.item("ae2:pattern_provider"),
                TcIngredient.item("thaumcraft:advanced_alchemical_construct"), TcIngredient.item("thaumcraft:essentia_crystallizer"),
                TcIngredient.item("ae2:engineering_processor"), TcIngredient.item("thaumcraft:alembic")
            )
        ));
        TeRecipes.register(new InfusionRecipe(
            CORE_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(CORE.getId().toString()), 3,
            new AspectList().add(Aspect.WATER, 16).add(Aspect.FIRE, 16).add(Aspect.MIND, 16).add(Aspect.EXCHANGE, 8),
            TcIngredient.item("thaumicenergistics:knowledge_core"),
            List.of(
                TcIngredient.item("thaumcraft:alembic"), TcIngredient.item("thaumcraft:salis_mundus"),
                TcIngredient.item("thaumcraft:alchemical_construct"), TcIngredient.item("ae2:calculation_processor")
            )
        ));
        TeRecipes.arcane(
            INSCRIBER_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(INSCRIBER.getId().toString()),
            new AspectList().add(Aspect.WATER, 10).add(Aspect.ORDER, 10).add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 10).add(Aspect.FIRE, 10).add(Aspect.AIR, 10),
            new String[] {"TPT", "CKC", "TLT"},
            'T', TcIngredient.item("thaumcraft:thaumium_ingot"),
            'P', TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            'C', TcIngredient.item("thaumcraft:alchemical_construct"), 'K', TcIngredient.item("thaumicenergistics:knowledge_inscriber"),
            'L', TcIngredient.item("ae2:engineering_processor")
        );
    }

    public static void research() {
        String key = TeResearch.CRUCIBLE_ASSEMBLER;
        TeResearch.item(
                key,
                new AspectList().add(Aspect.CRAFT, 5).add(Aspect.EXCHANGE, 5).add(Aspect.MECHANISM, 3).add(Aspect.WATER, 3).add(Aspect.MAGIC, 3),
                5, 1, TeResearch.COMPLEXITY_LARGE, TcStack.of(ASSEMBLER.getId().toString())
            )
            .setPages(
                TeResearch.text(key, 1),
                ResearchPage.infusion(ASSEMBLER_RECIPE),
                TeResearch.text(key, 2),
                ResearchPage.infusion(CORE_RECIPE),
                ResearchPage.arcane(INSCRIBER_RECIPE)
            )
            .setParents(TeResearch.ARCANE_ASSEMBLER)
            .setParentsHidden("CRUCIBLE", TeResearch.DISTILESSENTIA, "ADVALCHEMYFURNACE", "CENTRIFUGE", "ESSENTIACRYSTAL")
            .setConcealed()
            .registerResearchItem();
    }
}
