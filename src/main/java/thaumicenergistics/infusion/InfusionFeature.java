package thaumicenergistics.infusion;

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
import thaumicenergistics.Config;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeCapabilities;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class InfusionFeature {
    public static final String CORE_RECIPE = TeRecipes.key("ITEM_INFUSION_CORE");
    public static final String INSCRIBER_RECIPE = TeRecipes.key("BLOCK_INFUSION_INSCRIBER");
    public static final String ASSEMBLER_RECIPE = TeRecipes.key("BLOCK_INFUSION_ASSEMBLER");

    public static final TeStackKey<List<InfusionPattern>> PATTERNS = TeStackKey.register(
        "infusion_core_patterns", InfusionPattern.CODEC.listOf(), InfusionPattern.STREAM_CODEC.apply(ByteBufCodecs.list())
    );
    public static final TeStackKey<InfusionPattern> PATTERN = TeStackKey.register(
        "infusion_assembler_pattern", InfusionPattern.CODEC, InfusionPattern.STREAM_CODEC
    );
    public static final DeferredItem<InfusionCore> CORE = ModItems.ITEMS.registerItem("infusion_core", InfusionCore::new, properties -> properties);
    public static final DeferredBlock<InfusionInscriberBlock> INSCRIBER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "infusion_inscriber", InfusionInscriberBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionInscriberBlockEntity>> INSCRIBER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "infusion_inscriber", () -> BlockEntityType.Builder.of(InfusionInscriberBlockEntity::new, INSCRIBER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<InfusionInscriberMenu>> INSCRIBER_MENU = ModMenus.MENUS.register(
        "infusion_inscriber", () -> Menus.withPos(InfusionInscriberMenu::new)
    );
    public static final DeferredBlock<InfusionAssemblerBlock> ASSEMBLER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "infusion_assembler", InfusionAssemblerBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL).noOcclusion()
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionAssemblerBlockEntity>> ASSEMBLER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "infusion_assembler", () -> BlockEntityType.Builder.of(InfusionAssemblerBlockEntity::new, ASSEMBLER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<InfusionAssemblerMenu>> ASSEMBLER_MENU = ModMenus.MENUS.register(
        "infusion_assembler", () -> Menus.withPos(InfusionAssemblerMenu::new)
    );

    private InfusionFeature() {
    }

    public static void load() {
        TeCapabilities.gridHost(ASSEMBLER_ENTITY);
    }

    public static void setup() {
        AEBaseBlockEntity.registerBlockEntityItem(ASSEMBLER_ENTITY.get(), ASSEMBLER.asItem());
        Upgrades.add(AEItems.SPEED_CARD, ASSEMBLER.get(), InfusionAssemblerBlockEntity.MAX_SPEED_UPGRADES);
        PatternDetailsHelper.registerDecoder(new InfusionAssemblerPattern.Decoder());
    }

    public static void recipes() {
        TeRecipes.register(new InfusionRecipe(
            ASSEMBLER_RECIPE, TeResearch.INFUSION_ASSEMBLER, TcStack.of(ASSEMBLER.getId().toString()), 10,
            new AspectList().add(Aspect.CRAFT, 128).add(Aspect.EXCHANGE, 64).add(Aspect.MAGIC, 64).add(Aspect.AURA, 48).add(Aspect.ORDER, 48).add(Aspect.ELDRITCH, 16),
            TeRecipes.gated("thaumicenergistics:crucible_assembler", "infusion"),
            List.of(
                TcIngredient.item("thaumcraft:runic_matrix"), TcIngredient.item("thaumcraft:arcane_pedestal"),
                TcIngredient.item("thaumcraft:void_metal_ingot"), TcIngredient.item("ae2:singularity"),
                TcIngredient.item("thaumcraft:primordial_pearl"), TcIngredient.item("thaumcraft:arcane_pedestal"),
                TcIngredient.item("thaumcraft:void_metal_ingot"), TcIngredient.item("ae2:pattern_provider")
            )
        ));
        TeRecipes.register(new InfusionRecipe(
            CORE_RECIPE, TeResearch.INFUSION_ASSEMBLER, TcStack.of(CORE.getId().toString()), 4,
            new AspectList().add(Aspect.MAGIC, 24).add(Aspect.MIND, 24).add(Aspect.AURA, 16).add(Aspect.ORDER, 16),
            TeRecipes.gated("thaumicenergistics:alchemy_core", "infusion"),
            List.of(
                TcIngredient.item("thaumcraft:amber"), TcIngredient.item("thaumcraft:salis_mundus"),
                TcIngredient.item("thaumcraft:arcane_pedestal"), TcIngredient.item("ae2:engineering_processor")
            )
        ));
        TeRecipes.arcane(
            INSCRIBER_RECIPE, TeResearch.INFUSION_ASSEMBLER, TcStack.of(INSCRIBER.getId().toString()),
            new AspectList().add(Aspect.WATER, 20).add(Aspect.ORDER, 20).add(Aspect.EARTH, 20).add(Aspect.ENTROPY, 20).add(Aspect.FIRE, 20).add(Aspect.AIR, 20),
            new String[] {"VPV", "AKA", "VLV"},
            'V', TcIngredient.item("thaumcraft:void_metal_ingot"),
            'P', TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            'A', TcIngredient.item("thaumcraft:arcane_pedestal"), 'K', TeRecipes.gated("thaumicenergistics:alchemy_inscriber", "infusion"),
            'L', TcIngredient.item("ae2:engineering_processor")
        );
    }

    public static void research() {
        String key = TeResearch.INFUSION_ASSEMBLER;
        TeResearch.gated(
                Config::infusionAssembler, key,
                new AspectList().add(Aspect.CRAFT, 5).add(Aspect.EXCHANGE, 5).add(Aspect.MECHANISM, 3).add(Aspect.MAGIC, 5).add(Aspect.AURA, 3),
                6, 1, TeResearch.COMPLEXITY_LARGE, TcStack.of(ASSEMBLER.getId().toString())
            )
            .setPages(
                TeResearch.text(key, 1),
                ResearchPage.infusion(ASSEMBLER_RECIPE),
                TeResearch.text(key, 2),
                ResearchPage.infusion(CORE_RECIPE),
                ResearchPage.arcane(INSCRIBER_RECIPE)
            )
            .setParents(TeResearch.CRUCIBLE_ASSEMBLER)
            .setParentsHidden(TeResearch.INFUSION, "INFUSIONENCHANTMENT", "PRIMPEARL", "VOIDMETAL")
            .setConcealed()
            .registerResearchItem();
    }
}
