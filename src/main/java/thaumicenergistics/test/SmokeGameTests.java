package thaumicenergistics.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import thaumcraft.research.ResearchCategories;
import thaumicenergistics.ThaumicEnergistics;

public final class SmokeGameTests {
    private SmokeGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("smoke")
            .add("addon_and_base_loaded", SmokeGameTests::addonAndBaseLoaded)
            .add("base_research_category_exists", SmokeGameTests::baseResearchCategoryExists)
            .add("ae2_loaded", SmokeGameTests::ae2Loaded)
            .register(bus);
    }

    private static void addonAndBaseLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(ThaumicEnergistics.MODID), "addon is not loaded");
        helper.assertTrue(ModList.get().isLoaded("thaumcraft"), "base is not loaded");
        helper.succeed();
    }

    private static void baseResearchCategoryExists(GameTestHelper helper) {
        helper.assertTrue(ResearchCategories.get("BASICS") != null, "base research category BASICS is missing");
        helper.succeed();
    }

    private static void ae2Loaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("ae2"), "ae2 is not loaded");
        helper.succeed();
    }
}
