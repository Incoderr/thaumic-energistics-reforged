package thaumicenergistics.test;

import appeng.blockentity.misc.ChargerBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.gearbox.GearBoxBlockEntity;
import thaumicenergistics.gearbox.GearBoxFeature;
import thaumicenergistics.research.TeResearch;

public final class GearBoxGameTests {
    private static final BlockPos GEARBOX = new BlockPos(1, 1, 1);
    private static final BlockPos CHARGER = new BlockPos(1, 2, 1);

    private GearBoxGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("gearbox")
            .add("research_and_recipes", GearBoxGameTests::researchAndRecipes)
            .add("cranks_charger", GearBoxGameTests::cranksCharger)
            .register(bus);
    }

    private static void researchAndRecipes(GameTestHelper helper) {
        ResearchItem iron = ResearchCategories.getResearch(TeResearch.IRON_GEARBOX);
        helper.assertTrue(iron != null && iron.column() == 4 && iron.row() == -5 && iron.isSecondary(), "iron gearbox research is wrong");
        ResearchItem thaumium = ResearchCategories.getResearch(TeResearch.THAUMIUM_GEARBOX);
        helper.assertTrue(thaumium != null && thaumium.column() == 4 && thaumium.row() == -6, "thaumium gearbox research is wrong");
        helper.assertTrue(thaumium.parents().equals(List.of(TeResearch.IRON_GEARBOX, "thaumicenergistics.Pseudo.COREUSE")), "thaumium gearbox parents are " + thaumium.parents());
        helper.assertTrue(TcRecipes.get(GearBoxFeature.IRON_GEAR_RECIPE) != null && TcRecipes.get(GearBoxFeature.IRON_GEAR_BOX_RECIPE) != null
            && TcRecipes.get(GearBoxFeature.THAUMIUM_GEAR_BOX_RECIPE) != null, "gearbox recipes are missing");
        helper.succeed();
    }

    private static void cranksCharger(GameTestHelper helper) {
        helper.setBlock(GEARBOX, GearBoxFeature.IRON_GEAR_BOX.get());
        helper.setBlock(CHARGER, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:charger")));
        helper.runAfterDelay(2, () -> {
            GearBoxBlockEntity gearbox = (GearBoxBlockEntity) helper.getBlockEntity(GEARBOX);
            gearbox.updateCrankables();
            helper.assertTrue(gearbox.isSideFacingCrankable(Direction.UP), "gearbox does not see the charger");
            ChargerBlockEntity charger = (ChargerBlockEntity) helper.getBlockEntity(CHARGER);
            double before = charger.getAECurrentPower();
            for (int turn = 0; turn < 3; turn++) {
                helper.assertTrue(gearbox.crankGearbox(), "gearbox did not turn");
            }
            helper.assertTrue(charger.getAECurrentPower() > before, "charger got no power");
            helper.succeed();
        });
    }
}
