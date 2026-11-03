package thaumicenergistics.test;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.core.CoreFeature;
import thaumicenergistics.research.PseudoResearchItem;
import thaumicenergistics.research.TeResearch;

public final class CoreGameTests {
    private CoreGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("core")
            .add("category_and_basic_research", CoreGameTests::categoryAndBasicResearch)
            .add("cores_research", CoreGameTests::coresResearch)
            .add("storage_research", CoreGameTests::storageResearch)
            .add("pseudo_research_mirrors_real", CoreGameTests::pseudoResearchMirrorsReal)
            .add("recipes_registered", CoreGameTests::recipesRegistered)
            .add("vanilla_recipes_loaded", CoreGameTests::vanillaRecipesLoaded)
            .register(bus);
    }

    private static ResearchItem research(GameTestHelper helper, String key) {
        ResearchItem item = ResearchCategories.getResearch(key);
        helper.assertTrue(item != null, "research " + key + " is missing");
        return item;
    }

    private static void categoryAndBasicResearch(GameTestHelper helper) {
        helper.assertTrue(ResearchCategories.get(TeResearch.CATEGORY) != null, "research category is missing");
        ResearchItem basic = research(helper, "thaumicenergistics.TERESEARCH");
        helper.assertTrue(basic.isAutoUnlock() && basic.isRound(), "basic research flags are wrong");
        helper.assertTrue(basic.column() == 0 && basic.row() == 0, "basic research position is wrong");
        var category = ResearchCategories.get(TeResearch.CATEGORY);
        helper.assertTrue(category.minColumn() == -6 && category.maxColumn() == 6 && category.minRow() == -7 && category.maxRow() == 2, "category bounds do not match the full TE tree");
        helper.succeed();
    }

    private static void coresResearch(GameTestHelper helper) {
        ResearchItem cores = research(helper, "thaumicenergistics.TECORES");
        helper.assertTrue(cores.column() == 0 && cores.row() == -2 && cores.complexity() == 2, "cores position or complexity is wrong");
        helper.assertTrue(cores.parents().equals(List.of(TeResearch.BASIC, "thaumicenergistics.Pseudo.DISTILESSENTIA")), "cores parents are " + cores.parents());
        helper.assertTrue(cores.parentsHidden().equals(List.of("DISTILESSENTIA")), "cores hidden parents are " + cores.parentsHidden());
        helper.assertTrue(cores.pages().size() == 4, "cores has " + cores.pages().size() + " pages");
        helper.succeed();
    }

    private static void storageResearch(GameTestHelper helper) {
        ResearchItem storage = research(helper, "thaumicenergistics.TESTORAGE");
        helper.assertTrue(storage.column() == 0 && storage.row() == 2, "storage position is wrong");
        helper.assertTrue(storage.parents().equals(List.of(TeResearch.BASIC, "thaumicenergistics.Pseudo.DISTILESSENTIA", "thaumicenergistics.Pseudo.WARDEDARCANA")), "storage parents are " + storage.parents());
        helper.assertTrue(storage.pages().size() == 8, "storage has " + storage.pages().size() + " pages");
        helper.succeed();
    }

    private static void pseudoResearchMirrorsReal(GameTestHelper helper) {
        ResearchItem item = research(helper, "thaumicenergistics.Pseudo.DISTILESSENTIA");
        helper.assertTrue(item instanceof PseudoResearchItem, "pseudo research has the wrong class");
        ResearchItem real = research(helper, "DISTILESSENTIA");
        helper.assertTrue(item.isStub() && item.isHidden(), "pseudo research is not a hidden stub");
        helper.assertTrue(item.category().equals(TeResearch.CATEGORY) && item.column() == -2 && item.row() == 0, "pseudo research position is wrong");
        helper.assertTrue(item.nameKey().equals(real.nameKey()) && item.pages().equals(real.pages()), "pseudo research does not mirror the real one");
        helper.assertTrue(real.siblings().contains(item.key()), "real research does not unlock the pseudo research");
        helper.succeed();
    }

    private static void recipesRegistered(GameTestHelper helper) {
        List<String> keys = List.of(
            CoreFeature.COALESCENCE_CORE_RECIPE,
            CoreFeature.DIFFUSION_CORE_RECIPE,
            CellsFeature.HOUSING_RECIPE,
            CellsFeature.componentRecipe(EssentiaStorageType.TYPE_1K),
            CellsFeature.componentRecipe(EssentiaStorageType.TYPE_64K),
            CellsFeature.shapedCellRecipe(EssentiaStorageType.TYPE_16K),
            CellsFeature.shapelessCellRecipe(EssentiaStorageType.TYPE_4K)
        );
        for (String key : keys) {
            helper.assertTrue(TcRecipes.get(key) != null, "recipe " + key + " is missing");
        }
        helper.succeed();
    }

    private static void vanillaRecipesLoaded(GameTestHelper helper) {
        for (String name : List.of("essentia_cell_housing", "essentia_cell_1k", "essentia_cell_64k_shapeless")) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ThaumicEnergistics.id(name)).isPresent(), "vanilla recipe " + name + " is not loaded");
        }
        helper.succeed();
    }
}
