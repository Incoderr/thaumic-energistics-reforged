package thaumicenergistics.terminal;

import appeng.api.features.GridLinkables;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.implementations.MenuTypeBuilder;
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
import thaumicenergistics.platform.TeNetwork;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class TerminalFeature {
    public static final String TERMINAL_RECIPE = TeRecipes.key("PART_ESSENTIA_TERMINAL");
    public static final String WIRELESS_TERMINAL_RECIPE = TeRecipes.key("ITEM_WIRELESS_ESSENTIA_TERMINAL");
    public static final String LEVEL_EMITTER_RECIPE = TeRecipes.key("PART_ESSENTIA_LEVEL_EMITTER");
    public static final String STORAGE_MONITOR_RECIPE = TeRecipes.key("PART_ESSENTIA_STORAGE_MONITOR");
    public static final String CONVERSION_MONITOR_RECIPE = TeRecipes.key("PART_ESSENTIA_CONVERSION_MONITOR");

    public static final DeferredItem<PartItem<EssentiaTerminalPart>> TERMINAL = ModItems.ITEMS.registerItem(
        "essentia_terminal", properties -> new PartItem<>(properties, EssentiaTerminalPart.class, EssentiaTerminalPart::new), properties -> properties
    );
    public static final DeferredItem<WirelessEssentiaTerminalItem> WIRELESS_TERMINAL = ModItems.ITEMS.registerItem(
        "wireless_essentia_terminal", WirelessEssentiaTerminalItem::new, properties -> properties
    );
    public static final TeStackKey<EssentiaTerminalModes> MODES = TeStackKey.register("essentia_terminal_modes", EssentiaTerminalModes.CODEC, EssentiaTerminalModes.STREAM_CODEC);
    public static final DeferredHolder<MenuType<?>, MenuType<EssentiaTerminalMenu>> MENU = ModMenus.MENUS.register(
        "essentia_terminal",
        () -> MenuTypeBuilder.create(EssentiaTerminalMenu::new, EssentiaTerminalHost.class).buildUnregistered(ThaumicEnergistics.id("essentia_terminal"))
    );

    private TerminalFeature() {
    }

    public static void load() {
        PartModels.registerModels(EssentiaTerminalPart.MODEL_TERMINAL_BASE, EssentiaTerminalPart.MODEL_OFF, EssentiaTerminalPart.MODEL_ON);
    }

    public static void setup() {
        GridLinkables.register(WIRELESS_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
    }

    public static void payloads() {
        TeNetwork.toClient(EssentiaListPayload.TYPE, EssentiaListPayload.STREAM_CODEC);
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            TERMINAL_RECIPE, TeResearch.ESSENTIA_TERMINAL, TcStack.of(TERMINAL.getId().toString()),
            new AspectList().add(Aspect.WATER, 5).add(Aspect.ORDER, 2).add(Aspect.FIRE, 1),
            TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            TcIngredient.item(CoreFeature.DIFFUSION_CORE.getId().toString()),
            TcIngredient.item(CoreFeature.COALESCENCE_CORE.getId().toString()),
            TcIngredient.item("ae2:logic_processor"),
            TcIngredient.item("thaumcraft:vis_filter")
        );
        TeRecipes.vanilla(WIRELESS_TERMINAL_RECIPE, TeResearch.ESSENTIA_TERMINAL, "wireless_essentia_terminal", TcStack.of(WIRELESS_TERMINAL.getId().toString()));
    }

    public static void research() {
        TeResearch.item(
                TeResearch.ESSENTIA_TERMINAL,
                new AspectList().add(Aspect.EXCHANGE, 5).add(Aspect.SENSES, 5).add(Aspect.ENERGY, 3).add(Aspect.WATER, 3),
                -1, -4, TeResearch.COMPLEXITY_SMALL, TcStack.of(TERMINAL.getId().toString())
            )
            .setPages(TeResearch.existing(
                TeResearch.text(TeResearch.ESSENTIA_TERMINAL, 1),
                TeResearch.text(TeResearch.ESSENTIA_TERMINAL, 2),
                ResearchPage.arcane(TERMINAL_RECIPE),
                ResearchPage.crafting(WIRELESS_TERMINAL_RECIPE),
                TeResearch.text(TeResearch.ESSENTIA_TERMINAL, 3),
                ResearchPage.arcane(LEVEL_EMITTER_RECIPE),
                TeResearch.text(TeResearch.ESSENTIA_TERMINAL, 4),
                ResearchPage.crafting(STORAGE_MONITOR_RECIPE),
                ResearchPage.crafting(CONVERSION_MONITOR_RECIPE)
            ))
            .setParents(TeResearch.CORES)
            .setConcealed()
            .setSecondary()
            .registerResearchItem();
    }
}
