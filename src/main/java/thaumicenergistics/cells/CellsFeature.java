package thaumicenergistics.cells;

import appeng.api.storage.StorageCells;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class CellsFeature {
    public static final TeStackKey<EssentiaCellData> CELL_DATA = TeStackKey.register("essentia_cell", EssentiaCellData.CODEC, EssentiaCellData.STREAM_CODEC);

    public static final DeferredItem<Item> HOUSING = ModItems.ITEMS.registerItem("essentia_cell_housing", Item::new, properties -> properties);
    private static final Map<EssentiaStorageType, DeferredItem<Item>> COMPONENTS = new EnumMap<>(EssentiaStorageType.class);
    private static final Map<EssentiaStorageType, DeferredItem<EssentiaCellItem>> CELLS = new EnumMap<>(EssentiaStorageType.class);

    public static final String HOUSING_RECIPE = TeRecipes.key("ITEM_STORAGE_HOUSING");

    static {
        for (EssentiaStorageType type : EssentiaStorageType.values()) {
            if (type.isCreative()) {
                continue;
            }
            COMPONENTS.put(type, ModItems.ITEMS.registerItem("essentia_storage_component_" + type.suffix(), Item::new, properties -> properties.rarity(type.rarity())));
        }
        for (EssentiaStorageType type : EssentiaStorageType.values()) {
            String name = type.isCreative() ? "creative_essentia_cell" : "essentia_cell_" + type.suffix();
            CELLS.put(type, ModItems.ITEMS.registerItem(name, properties -> new EssentiaCellItem(properties, type), properties -> properties));
        }
    }

    private CellsFeature() {
    }

    public static DeferredItem<Item> component(EssentiaStorageType type) {
        return COMPONENTS.get(type);
    }

    public static DeferredItem<EssentiaCellItem> cell(EssentiaStorageType type) {
        return CELLS.get(type);
    }

    public static void load() {
        StorageCells.addCellHandler(EssentiaCellHandler.INSTANCE);
    }

    public static String componentRecipe(EssentiaStorageType type) {
        return TeRecipes.key("ITEM_STORAGE_COMPONENT_" + type.suffix().toUpperCase());
    }

    public static String shapedCellRecipe(EssentiaStorageType type) {
        return TeRecipes.key("ITEM_STORAGE_CELL_" + type.suffix().toUpperCase() + "_SHAPED");
    }

    public static String shapelessCellRecipe(EssentiaStorageType type) {
        return TeRecipes.key("ITEM_STORAGE_CELL_" + type.suffix().toUpperCase() + "_SHAPELESS");
    }

    public static void recipes() {
        TcIngredient essence = TcIngredient.item("thaumcraft:wisp_essence");
        TcIngredient salis = TcIngredient.item("thaumcraft:salis_mundus");
        TcIngredient quartzGlass = TcIngredient.item("ae2:quartz_glass");
        TeRecipes.arcane(
            componentRecipe(EssentiaStorageType.TYPE_1K), TeResearch.STORAGE, stack(component(EssentiaStorageType.TYPE_1K)),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.ORDER, 1),
            new String[] {"EQ ", "QPQ", " QE"},
            'E', essence, 'Q', TcIngredient.tag("ae2:all_certus_quartz"), 'P', TcIngredient.item("ae2:logic_processor")
        );
        TeRecipes.arcane(
            componentRecipe(EssentiaStorageType.TYPE_4K), TeResearch.STORAGE, stack(component(EssentiaStorageType.TYPE_4K)),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.ORDER, 2),
            new String[] {"EPE", "1G1", "E1E"},
            'E', essence, '1', ingredient(component(EssentiaStorageType.TYPE_1K)), 'P', TcIngredient.item("ae2:calculation_processor"), 'G', quartzGlass
        );
        TeRecipes.arcane(
            componentRecipe(EssentiaStorageType.TYPE_16K), TeResearch.STORAGE, stack(component(EssentiaStorageType.TYPE_16K)),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.ORDER, 4),
            new String[] {"SPE", "4G4", "E4S"},
            'E', essence, 'S', salis, '4', ingredient(component(EssentiaStorageType.TYPE_4K)), 'P', TcIngredient.item("ae2:engineering_processor"), 'G', quartzGlass
        );
        TeRecipes.arcane(
            componentRecipe(EssentiaStorageType.TYPE_64K), TeResearch.STORAGE, stack(component(EssentiaStorageType.TYPE_64K)),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.ORDER, 8),
            new String[] {"SPS", "6G6", "S6S"},
            'S', salis, '6', ingredient(component(EssentiaStorageType.TYPE_16K)), 'P', TcIngredient.item("ae2:engineering_processor"), 'G', quartzGlass
        );
        TeRecipes.vanilla(HOUSING_RECIPE, TeResearch.STORAGE, "essentia_cell_housing", stack(HOUSING));
        for (EssentiaStorageType type : COMPONENTS.keySet()) {
            String name = "essentia_cell_" + type.suffix();
            TeRecipes.vanilla(shapedCellRecipe(type), TeResearch.STORAGE, name, stack(cell(type)));
            TeRecipes.vanilla(shapelessCellRecipe(type), TeResearch.STORAGE, name + "_shapeless", stack(cell(type)));
        }
    }

    public static void research() {
        TeResearch.item(
                TeResearch.STORAGE,
                new AspectList().add(Aspect.VOID, 5).add(Aspect.ENERGY, 5).add(Aspect.CRYSTAL, 3).add(Aspect.METAL, 3),
                0, 2, TeResearch.COMPLEXITY_MEDIUM, stack(cell(EssentiaStorageType.TYPE_64K))
            )
            .setPages(
                TeResearch.text(TeResearch.STORAGE, 1),
                TeResearch.text(TeResearch.STORAGE, 2),
                ResearchPage.arcane(componentRecipe(EssentiaStorageType.TYPE_1K)),
                ResearchPage.arcane(componentRecipe(EssentiaStorageType.TYPE_4K), componentRecipe(EssentiaStorageType.TYPE_16K), componentRecipe(EssentiaStorageType.TYPE_64K)),
                ResearchPage.crafting(HOUSING_RECIPE),
                ResearchPage.crafting(COMPONENTS.keySet().stream().map(CellsFeature::shapedCellRecipe).toArray(String[]::new)),
                ResearchPage.crafting(COMPONENTS.keySet().stream().map(CellsFeature::shapelessCellRecipe).toArray(String[]::new)),
                ResearchPage.crafting(WorkbenchFeature.RECIPE)
            )
            .setParents(TeResearch.BASIC, TeResearch.pseudo(TeResearch.DISTILESSENTIA), TeResearch.pseudo(TeResearch.WARDED))
            .setParentsHidden(TeResearch.DISTILESSENTIA)
            .registerResearchItem();
    }

    private static TcStack stack(DeferredItem<? extends Item> item) {
        return TcStack.of(item.getId().toString());
    }

    private static TcIngredient ingredient(DeferredItem<? extends Item> item) {
        return TcIngredient.of(stack(item));
    }
}
