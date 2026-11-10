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
            ASSEMBLER_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(ASSEMBLER.getId().toString()), 5,
            new AspectList().add(Aspect.CRAFT, 32).add(Aspect.EXCHANGE, 32).add(Aspect.MAGIC, 16).add(Aspect.WATER, 16).add(Aspect.FIRE, 8),
            TcIngredient.item("ae2:molecular_assembler"),
            List.of(
                TcIngredient.item("thaumcraft:crucible"), TcIngredient.item("thaumcraft:alchemical_construct"),
                TcIngredient.item("thaumcraft:alembic"), TcIngredient.item("thaumcraft:essentia_tube"),
                TcIngredient.item("ae2:pattern_provider"), TcIngredient.item("thaumcraft:alchemical_construct"),
                TcIngredient.item("thaumcraft:thaumium_ingot"), TcIngredient.item("thaumcraft:essentia_tube")
            )
        ));
        TeRecipes.arcane(
            CORE_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(CORE.getId().toString()),
            new AspectList().add(Aspect.WATER, 3).add(Aspect.ORDER, 3).add(Aspect.FIRE, 1),
            new String[] {"VGV", "GAG", "VCV"},
            'V', TcIngredient.item("ae2:quartz_vibrant_glass"), 'G', TcIngredient.item("minecraft:glowstone_dust"),
            'A', TcIngredient.item("thaumcraft:alembic"), 'C', TcIngredient.item("ae2:calculation_processor")
        );
        TeRecipes.arcane(
            INSCRIBER_RECIPE, TeResearch.CRUCIBLE_ASSEMBLER, TcStack.of(INSCRIBER.getId().toString()),
            new AspectList().add(Aspect.WATER, 5).add(Aspect.ORDER, 5).add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 5).add(Aspect.AIR, 5),
            new String[] {"IPI", " T ", "ILI"},
            'I', TcIngredient.item("thaumcraft:thaumium_ingot"),
            'P', TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            'T', TcIngredient.item("thaumcraft:crucible"), 'L', TcIngredient.item("ae2:logic_processor")
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
                ResearchPage.arcane(CORE_RECIPE),
                ResearchPage.arcane(INSCRIBER_RECIPE)
            )
            .setParents(TeResearch.ARCANE_ASSEMBLER)
            .setParentsHidden("CRUCIBLE", TeResearch.DISTILESSENTIA)
            .setConcealed()
            .registerResearchItem();
    }
}
