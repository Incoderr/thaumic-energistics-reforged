package thaumicenergistics.vibration;

import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaTileContainers;
import thaumicenergistics.platform.TeCapabilities;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class VibrationFeature {
    public static final String RECIPE = TeRecipes.key("BLOCK_ESSENTIA_VIBRATION_CHAMBER");

    public static final DeferredBlock<VibrationChamberBlock> CHAMBER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "essentia_vibration_chamber", VibrationChamberBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VibrationChamberBlockEntity>> ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "essentia_vibration_chamber", () -> BlockEntityType.Builder.of(VibrationChamberBlockEntity::new, CHAMBER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<VibrationChamberMenu>> MENU = ModMenus.MENUS.register(
        "essentia_vibration_chamber", () -> Menus.withPos(VibrationChamberMenu::new)
    );

    private VibrationFeature() {
    }

    public static void load() {
        TeCapabilities.gridHost(ENTITY);
        EssentiaTileContainers.injectOnly(VibrationChamberBlockEntity.class, VibrationChamberBlockEntity.MAX_ESSENTIA_STORED);
        EssentiaContainers.registerRestricted(CHAMBER.getId(), VibrationChamberBlockEntity.MAX_ESSENTIA_STORED, VibrationChamberBlockEntity::acceptsAspect);
    }

    public static void setup() {
        AEBaseBlockEntity.registerBlockEntityItem(ENTITY.get(), CHAMBER.asItem());
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            RECIPE, TeResearch.VIBRATION_CHAMBER, TcStack.of(CHAMBER.getId().toString()),
            new AspectList().add(Aspect.WATER, 2).add(Aspect.ENTROPY, 4).add(Aspect.FIRE, 7).add(Aspect.ORDER, 4),
            TcIngredient.item("ae2:vibration_chamber"), TcIngredient.item(CoreFeature.DIFFUSION_CORE.getId().toString()),
            TcIngredient.item("thaumcraft:warded_jar")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.VIBRATION_CHAMBER,
                new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.MAGIC, 3).add(Aspect.WATER, 1).add(Aspect.MOTION, 3).add(Aspect.ENERGY, 5),
                1, -4, TeResearch.COMPLEXITY_MEDIUM, TcStack.of(CHAMBER.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.VIBRATION_CHAMBER, 1),
                ResearchPage.arcane(RECIPE),
                TeResearch.text(TeResearch.VIBRATION_CHAMBER, 2),
                TeResearch.text(TeResearch.VIBRATION_CHAMBER, 3)
            )
            .setParents(TeResearch.CORES)
            .setConcealed()
            .registerResearchItem();
    }
}
