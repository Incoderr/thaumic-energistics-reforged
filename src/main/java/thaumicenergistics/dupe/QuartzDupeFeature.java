package thaumicenergistics.dupe;

import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.research.TeResearch;

public final class QuartzDupeFeature {
    public static final String DUPE_CERTUS = TeRecipes.key("DUPE_CERTUS");
    public static final String DUPE_NETHER_QUARTZ = TeRecipes.key("DUPE_NETHER_QUARTZ");
    private static final String CERTUS = "ae2:certus_quartz_crystal";

    private QuartzDupeFeature() {
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.register(new CrucibleRecipe(
            DUPE_CERTUS, TeResearch.CERTUS_DUPE, TcStack.of(CERTUS, 2),
            TcIngredient.tag("c:gems/certus_quartz"), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.WATER, 2)
        ));
        TeRecipes.register(new CrucibleRecipe(
            DUPE_NETHER_QUARTZ, TeResearch.CERTUS_DUPE, TcStack.of("minecraft:quartz", 2),
            TcIngredient.tag("c:gems/quartz"), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.WATER, 2).add(Aspect.ENERGY, 2)
        ));
    }

    public static void research() {
        TeResearch.item(TeResearch.CERTUS_DUPE, new AspectList().add(Aspect.CRYSTAL, 5), -5, -5, TeResearch.COMPLEXITY_SMALL, TcStack.of(CERTUS))
            .setPages(TeResearch.text(TeResearch.CERTUS_DUPE, 1), ResearchPage.crucible(DUPE_CERTUS), ResearchPage.crucible(DUPE_NETHER_QUARTZ))
            .setParents(TeResearch.pseudo(TeResearch.DUPE))
            .setParentsHidden(TeResearch.DUPE)
            .setSecondary()
            .setConcealed()
            .setItemTriggers(CERTUS)
            .registerResearchItem();
    }
}
