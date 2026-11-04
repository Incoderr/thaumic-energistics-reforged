package thaumicenergistics.buses;

import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartItem;
import appeng.menu.implementations.MenuTypeBuilder;
import java.util.List;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class BusesFeature {
    public static final String IMPORT_BUS_RECIPE = TeRecipes.key("PART_IMPORT_BUS");
    public static final String EXPORT_BUS_RECIPE = TeRecipes.key("PART_EXPORT_BUS");
    public static final String STORAGE_BUS_RECIPE = TeRecipes.key("PART_STORAGE_BUS");
    public static final String IO_UPGRADE_GROUP = "thaumicenergistics.group.essentia.transport";

    public static final DeferredItem<PartItem<EssentiaImportBusPart>> IMPORT_BUS = ModItems.ITEMS.registerItem(
        "essentia_import_bus", properties -> new PartItem<>(properties, EssentiaImportBusPart.class, EssentiaImportBusPart::new), properties -> properties
    );
    public static final DeferredItem<PartItem<EssentiaStorageBusPart>> STORAGE_BUS = ModItems.ITEMS.registerItem(
        "essentia_storage_bus", properties -> new PartItem<>(properties, EssentiaStorageBusPart.class, EssentiaStorageBusPart::new), properties -> properties
    );
    public static final DeferredItem<PartItem<EssentiaExportBusPart>> EXPORT_BUS = ModItems.ITEMS.registerItem(
        "essentia_export_bus", properties -> new PartItem<>(properties, EssentiaExportBusPart.class, EssentiaExportBusPart::new), properties -> properties
    );
    public static final DeferredHolder<MenuType<?>, MenuType<EssentiaBusMenu>> MENU = ModMenus.MENUS.register(
        "essentia_bus",
        () -> MenuTypeBuilder.create(EssentiaBusMenu::new, EssentiaBusHost.class).buildUnregistered(ThaumicEnergistics.id("essentia_bus"))
    );

    private BusesFeature() {
    }

    public static void load() {
        PartModels.registerModels(
            EssentiaImportBusPart.MODEL_BASE, EssentiaImportBusPart.MODEL_OFF, EssentiaImportBusPart.MODEL_ON,
            EssentiaExportBusPart.MODEL_BASE, EssentiaExportBusPart.MODEL_OFF, EssentiaExportBusPart.MODEL_ON,
            EssentiaStorageBusPart.MODEL_BASE, EssentiaStorageBusPart.MODEL_OFF, EssentiaStorageBusPart.MODEL_ON
        );
    }

    public static void setup() {
        for (PartItem<?> bus : List.of(IMPORT_BUS.get(), EXPORT_BUS.get())) {
            Upgrades.add(AEItems.CAPACITY_CARD, bus, 2, IO_UPGRADE_GROUP);
            Upgrades.add(AEItems.REDSTONE_CARD, bus, 1, IO_UPGRADE_GROUP);
            Upgrades.add(AEItems.SPEED_CARD, bus, 2, IO_UPGRADE_GROUP);
        }
        Upgrades.add(AEItems.INVERTER_CARD, STORAGE_BUS.get(), 1);
    }

    public static void recipes() {
        TcIngredient diffusion = TcIngredient.item(CoreFeature.DIFFUSION_CORE.getId().toString());
        TcIngredient coalescence = TcIngredient.item(CoreFeature.COALESCENCE_CORE.getId().toString());
        TcIngredient iron = TcIngredient.tag("c:ingots/iron");
        TcIngredient filterTube = TcIngredient.item("thaumcraft:filtered_essentia_tube");
        TcIngredient wardedJar = TcIngredient.item("thaumcraft:warded_jar");
        AspectList ioAspects = new AspectList().add(Aspect.FIRE, 2).add(Aspect.EARTH, 2).add(Aspect.WATER, 1);
        TeRecipes.arcane(
            STORAGE_BUS_RECIPE, TeResearch.IO, TcStack.of(STORAGE_BUS.getId().toString()),
            new AspectList().add(Aspect.FIRE, 3).add(Aspect.EARTH, 3).add(Aspect.WATER, 1),
            new String[] {"DFC", "IWI"},
            'D', diffusion, 'F', filterTube, 'C', coalescence, 'I', iron, 'W', TcIngredient.item("thaumcraft:warded_glass")
        );
        TeRecipes.arcane(
            IMPORT_BUS_RECIPE, TeResearch.IO, TcStack.of(IMPORT_BUS.getId().toString()), ioAspects,
            new String[] {"JDJ", "IFI"},
            'J', wardedJar, 'D', diffusion, 'I', iron, 'F', filterTube
        );
        TeRecipes.arcane(
            EXPORT_BUS_RECIPE, TeResearch.IO, TcStack.of(EXPORT_BUS.getId().toString()), ioAspects,
            new String[] {"JCJ", "IFI"},
            'J', wardedJar, 'C', coalescence, 'I', iron, 'F', filterTube
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.IO,
                new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.METAL, 3).add(Aspect.CRYSTAL, 3).add(Aspect.AIR, 3),
                -2, -2, TeResearch.COMPLEXITY_MEDIUM, TcStack.of(EXPORT_BUS.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.IO, 1),
                TeResearch.text(TeResearch.IO, 2),
                ResearchPage.arcane(IMPORT_BUS_RECIPE),
                ResearchPage.arcane(EXPORT_BUS_RECIPE),
                ResearchPage.arcane(STORAGE_BUS_RECIPE)
            )
            .setParents(TeResearch.CORES, TeResearch.pseudo(TeResearch.TUBEFILTER))
            .setParentsHidden(TeResearch.TUBEFILTER)
            .setConcealed()
            .registerResearchItem();
    }
}
