package thaumicenergistics.act;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
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
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class ActFeature {
    public static final String RECIPE = TeRecipes.key("PART_ARCANE_TERMINAL");

    public static final DeferredItem<PartItem<ArcaneCraftingTerminalPart>> TERMINAL = ModItems.ITEMS.registerItem(
        "arcane_crafting_terminal", properties -> new PartItem<>(properties, ArcaneCraftingTerminalPart.class, ArcaneCraftingTerminalPart::new), properties -> properties
    );
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneCraftingTermMenu>> MENU = ModMenus.MENUS.register(
        "arcane_crafting_terminal",
        () -> MenuTypeBuilder.create(ArcaneCraftingTermMenu::new, ArcaneCraftingTerminalPart.class).buildUnregistered(ThaumicEnergistics.id("arcane_crafting_terminal"))
    );

    private ActFeature() {
    }

    public static void load() {
        PartModels.registerModels(ArcaneCraftingTerminalPart.MODEL_BASE, ArcaneCraftingTerminalPart.MODEL_OFF, ArcaneCraftingTerminalPart.MODEL_ON);
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            RECIPE, TeResearch.ARCANE_TERMINAL, TcStack.of(TERMINAL.getId().toString()),
            new AspectList().add(Aspect.AIR, 10).add(Aspect.EARTH, 10).add(Aspect.ENTROPY, 10).add(Aspect.FIRE, 10).add(Aspect.ORDER, 10).add(Aspect.WATER, 10),
            TcIngredient.item("ae2:terminal"),
            TcIngredient.item("thaumcraft:arcane_worktable"),
            TcIngredient.item("ae2:calculation_processor")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.ARCANE_TERMINAL,
                new AspectList().add(Aspect.CRAFT, 5).add(Aspect.ENERGY, 3).add(Aspect.MECHANISM, 3).add(Aspect.VOID, 3),
                2, -1, TeResearch.COMPLEXITY_SMALL, TcStack.of(TERMINAL.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.ARCANE_TERMINAL, 1), ResearchPage.arcane(RECIPE))
            .setParents(TeResearch.BASIC)
            .registerResearchItem();
    }
}
