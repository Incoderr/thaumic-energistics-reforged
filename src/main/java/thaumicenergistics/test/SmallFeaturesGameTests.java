package thaumicenergistics.test;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectRegistry;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.dupe.QuartzDupeFeature;
import thaumicenergistics.gearbox.GearBoxFeature;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.wrench.WrenchFocusFeature;

public final class SmallFeaturesGameTests {
    private SmallFeaturesGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("small")
            .add("quartz_dupe", SmallFeaturesGameTests::quartzDupe)
            .add("wrench_focus", SmallFeaturesGameTests::wrenchFocus)
            .add("ae_aspects", SmallFeaturesGameTests::aeAspects)
            .add("thaumcraft_facades", SmallFeaturesGameTests::facades)
            .register(bus);
    }

    private static Item ae(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ae2", path));
    }

    private static void quartzDupe(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch(TeResearch.CERTUS_DUPE);
        helper.assertTrue(item != null && item.column() == -5 && item.row() == -5 && item.isSecondary(), "quartz dupe research is wrong");
        helper.assertTrue(item.parents().equals(List.of("thaumicenergistics.Pseudo.ALCHEMICALDUPLICATION")), "quartz dupe parents are " + item.parents());
        helper.assertTrue(TcRecipes.get(QuartzDupeFeature.DUPE_CERTUS) != null && TcRecipes.get(QuartzDupeFeature.DUPE_NETHER_QUARTZ) != null, "dupe recipes are missing");
        helper.succeed();
    }

    private static void wrenchFocus(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch(TeResearch.FOCUS_WRENCH);
        helper.assertTrue(item != null && item.column() == -3 && item.row() == -7 && item.isSecondary(), "wrench focus research is wrong");
        helper.assertTrue(item.parents().equals(List.of("thaumicenergistics.Pseudo.FOCUSFIRE")), "wrench focus parents are " + item.parents());
        helper.assertTrue(TcRecipes.get(WrenchFocusFeature.WRENCH_FOCUS_RECIPE) != null, "wrench focus recipe is missing");
        AspectList cost = WrenchFocusFeature.FOCUS.get().getVisCost(WrenchFocusFeature.FOCUS.toStack());
        helper.assertTrue(cost.getAmount(Aspect.FIRE) == 10 && cost.getAmount(Aspect.AIR) == 10 && cost.size() == 2, "wrench focus cost is " + cost);
        helper.succeed();
    }

    private static void aeAspects(GameTestHelper helper) {
        AspectRegistry registry = AspectRegistry.active();
        AspectList certus = registry.getObjectTags(ae("certus_quartz_crystal"));
        helper.assertTrue(certus != null && certus.size() == 1 && certus.getAmount(Aspect.CRYSTAL) == 2, "certus aspects are " + certus);
        AspectList singularity = registry.getObjectTags(ae("singularity"));
        helper.assertTrue(singularity != null && singularity.getAmount(Aspect.VOID) == 10, "singularity aspects are " + singularity);
        AspectList processor = registry.getObjectTags(ae("logic_processor"));
        helper.assertTrue(processor != null && processor.getAmount(Aspect.MIND) >= 3 && processor.size() <= 6, "logic processor aspects are " + processor);
        AspectList controller = registry.getObjectTags(ae("controller"));
        helper.assertTrue(controller != null && controller.getAmount(Aspect.MIND) >= 9, "controller aspects are " + controller);
        AspectList cell = registry.getObjectTags(CellsFeature.cell(EssentiaStorageType.values()[0]).get());
        helper.assertTrue(cell != null && cell.getAmount(Aspect.AURA) >= 5, "essentia cell aspects are " + cell);
        AspectList gearbox = registry.getObjectTags(GearBoxFeature.IRON_GEAR_BOX.asItem());
        helper.assertTrue(gearbox != null && gearbox.getAmount(Aspect.METAL) == 7 && gearbox.getAmount(Aspect.MECHANISM) == 5, "iron gearbox aspects are " + gearbox);
        helper.succeed();
    }

    private static void facades(GameTestHelper helper) {
        TagKey<Block> whitelist = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("ae2", "whitelisted/facades"));
        for (String name : List.of("amber_block", "arcane_stone_bricks", "greatwood_planks", "flesh_block")) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", name));
            helper.assertTrue(block.defaultBlockState().is(whitelist), name + " is not a facade block");
        }
        helper.succeed();
    }
}
