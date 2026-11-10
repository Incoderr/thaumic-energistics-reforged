package thaumicenergistics.assembler;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import java.util.List;
import java.util.Map;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.WandParts;
import thaumcraft.wand.Wands;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.platform.TeCapabilities;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class AssemblerFeature {
    private static final String SCEPTRE_DATA = "sceptre";
    private static final String FULL_VIS_DATA = "thaumicenergistics_assembler_vis";

    public static final TeStackKey<ArcanePattern> PATTERN = TeStackKey.register(
        "arcane_assembler_pattern", ArcanePattern.CODEC, ArcanePattern.STREAM_CODEC
    );
    public static final TeStackKey<AspectList> STORED_VIS = TeStackKey.register(
        "arcane_assembler_vis", AspectList.CODEC, AspectList.STREAM_CODEC.cast()
    );
    public static final DeferredBlock<ArcaneAssemblerBlock> BLOCK = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "arcane_assembler", ArcaneAssemblerBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL).noOcclusion()
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneAssemblerBlockEntity>> ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "arcane_assembler", () -> BlockEntityType.Builder.of(ArcaneAssemblerBlockEntity::new, BLOCK.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneAssemblerMenu>> MENU = ModMenus.MENUS.register(
        "arcane_assembler", () -> Menus.withPos(ArcaneAssemblerMenu::new)
    );

    private AssemblerFeature() {
    }

    public static void load() {
        TeCapabilities.gridHost(ENTITY);
        TcStack.registerDataHandler(SCEPTRE_DATA, (stack, value) -> {
            WandParts parts = Wands.getParts(stack);
            WandKeys.PARTS.set(stack, new WandParts(parts.cap(), parts.rod(), Boolean.parseBoolean(value)));
        });
        TcStack.registerDataMatcher(SCEPTRE_DATA, (stack, value) -> Wands.isSceptre(stack) == Boolean.parseBoolean(value));
        TcStack.registerDataHandler(FULL_VIS_DATA, (stack, value) -> STORED_VIS.set(stack, ArcaneAssemblerBlockEntity.fullVis()));
    }

    public static void setup() {
        AEBaseBlockEntity.registerBlockEntityItem(ENTITY.get(), BLOCK.asItem());
        Upgrades.add(AEItems.SPEED_CARD, BLOCK.get(), ArcaneAssemblerBlockEntity.MAX_SPEED_UPGRADES);
        PatternDetailsHelper.registerDecoder(new AssemblerPattern.Decoder());
    }

    public static void recipes() {
        TcIngredient sceptre = TcIngredient.of(TcStack.withData("thaumcraft:wand", 1, Map.of("cap", "thaumium", "rod", "silverwood", SCEPTRE_DATA, "true")));
        AspectList aspects = new AspectList().add(Aspect.CRAFT, 64).add(Aspect.EXCHANGE, 32).add(Aspect.AURA, 16).add(Aspect.MAGIC, 16)
            .add(Aspect.METAL, 8).add(Aspect.CRYSTAL, 8);
        TeRecipes.register(new InfusionRecipe(
            KnowledgeFeature.ASSEMBLER_RECIPE, TeResearch.ARCANE_ASSEMBLER,
            TcStack.withData(BLOCK.getId().toString(), 1, Map.of(FULL_VIS_DATA, "true")), 7, aspects,
            TcIngredient.item("ae2:molecular_assembler"),
            List.of(
                sceptre, TcIngredient.item("thaumcraft:air_shard"), TcIngredient.item("thaumcraft:earth_shard"),
                TcIngredient.item("thaumcraft:water_shard"), TcIngredient.item("thaumcraft:balanced_shard"),
                TcIngredient.item("thaumcraft:order_shard"), TcIngredient.item("thaumcraft:entropy_shard"),
                TcIngredient.item("thaumcraft:fire_shard")
            )
        ));
    }
}
