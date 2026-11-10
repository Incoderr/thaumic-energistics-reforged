package thaumicenergistics.wrench;

import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.focus.FocusItem;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;

public final class WrenchFocusFeature {
    public static final String WRENCH_FOCUS_RECIPE = TeRecipes.key("ITEM_WRENCH_FOCUS");
    public static final DeferredItem<AeWrenchFocusItem> FOCUS = ModItems.ITEMS.registerItem("ae_wrench_focus", AeWrenchFocusItem::new, FocusItem::properties);

    private WrenchFocusFeature() {
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.arcane(
            WRENCH_FOCUS_RECIPE, TeResearch.FOCUS_WRENCH, TcStack.of(FOCUS.getId().toString()),
            new AspectList().add(Aspect.AIR, 10).add(Aspect.FIRE, 10),
            new String[] {"ANF", "NWN", "FNA"},
            'A', TcIngredient.item("thaumcraft:air_shard"),
            'F', TcIngredient.item("thaumcraft:fire_shard"),
            'N', TcIngredient.tag("c:gems/quartz"),
            'W', TcIngredient.item("ae2:certus_quartz_wrench")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.FOCUS_WRENCH,
                new AspectList().add(Aspect.MECHANISM, 6).add(Aspect.TOOL, 5).add(Aspect.MAGIC, 3),
                -3, -7, TeResearch.COMPLEXITY_SMALL, TcStack.of(FOCUS.getId().toString())
            )
            .setPages(TeResearch.text(TeResearch.FOCUS_WRENCH, 1), ResearchPage.arcane(WRENCH_FOCUS_RECIPE))
            .setParents(TeResearch.pseudo(TeResearch.FOCUSFIRE))
            .setSecondary()
            .registerResearchItem();
    }
}
