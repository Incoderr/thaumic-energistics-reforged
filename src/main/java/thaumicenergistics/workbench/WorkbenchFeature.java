package thaumicenergistics.workbench;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeNetwork;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class WorkbenchFeature {
    public static final String RECIPE = TeRecipes.key("BLOCK_CELL_WORKBENCH");

    public static final DeferredBlock<EssentiaCellWorkbenchBlock> BLOCK = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "essentia_cell_workbench", EssentiaCellWorkbenchBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaCellWorkbenchBlockEntity>> BLOCK_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "essentia_cell_workbench", () -> BlockEntityType.Builder.of(EssentiaCellWorkbenchBlockEntity::new, BLOCK.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<EssentiaCellWorkbenchMenu>> MENU = ModMenus.MENUS.register(
        "essentia_cell_workbench", () -> Menus.withPos(EssentiaCellWorkbenchMenu::new)
    );

    private WorkbenchFeature() {
    }

    public static void load() {
    }

    public static void payloads() {
        TeNetwork.toServer(WorkbenchActionPayload.TYPE, WorkbenchActionPayload.STREAM_CODEC, WorkbenchActionPayload::handle);
        TeNetwork.toClient(WorkbenchPartitionPayload.TYPE, WorkbenchPartitionPayload.STREAM_CODEC);
    }

    public static void recipes() {
        TeRecipes.vanilla(RECIPE, TeResearch.STORAGE, "essentia_cell_workbench", TcStack.of(BLOCK.getId().toString()));
    }
}
