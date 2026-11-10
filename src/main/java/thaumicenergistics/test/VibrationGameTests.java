package thaumicenergistics.test;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaTileContainers;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.vibration.VibrationChamberBlock;
import thaumicenergistics.vibration.VibrationChamberBlockEntity;
import thaumicenergistics.vibration.VibrationFeature;

public final class VibrationGameTests {
    private static final BlockPos CHAMBER = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE = new BlockPos(2, 1, 1);
    private static final BlockPos ENERGY = new BlockPos(3, 1, 1);

    private VibrationGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("vibration")
            .add("research_and_recipe", VibrationGameTests::researchAndRecipe)
            .add("container_rules", VibrationGameTests::containerRules)
            .add("item_container", VibrationGameTests::itemContainer)
            .add("produces_power", VibrationGameTests::producesPower)
            .register(bus);
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        ResearchItem research = ResearchCategories.getResearch(TeResearch.VIBRATION_CHAMBER);
        helper.assertTrue(research != null && research.column() == 1 && research.row() == -4 && research.complexity() == 2, "chamber research is wrong");
        helper.assertTrue(research.parents().equals(List.of(TeResearch.CORES)), "chamber parents are " + research.parents());
        helper.assertTrue(TcRecipes.get(VibrationFeature.RECIPE) != null, "chamber recipe is missing");
        helper.succeed();
    }

    private static void containerRules(GameTestHelper helper) {
        helper.setBlock(CHAMBER, VibrationFeature.CHAMBER.get());
        VibrationChamberBlockEntity chamber = (VibrationChamberBlockEntity) helper.getBlockEntity(CHAMBER);
        helper.assertTrue(EssentiaTileContainers.inject(chamber, 10, Aspect.WATER, true) == 0, "chamber took aqua");
        helper.assertTrue(EssentiaTileContainers.inject(chamber, 100, Aspect.FIRE, true) == 64 && chamber.storedAmount() == 64, "chamber did not take 64 ignis");
        helper.assertTrue(EssentiaTileContainers.inject(chamber, 10, Aspect.ENERGY, true) == 0, "chamber mixed aspects");
        helper.assertTrue(EssentiaTileContainers.extract(chamber, 10, Aspect.FIRE, true) == 0 && chamber.storedAmount() == 64, "chamber gave essentia out");
        helper.succeed();
    }

    private static void itemContainer(GameTestHelper helper) {
        ItemStack item = new ItemStack(VibrationFeature.CHAMBER.get());
        helper.assertTrue(EssentiaContainers.fill(item, Aspect.WATER, 10) == null, "chamber item took aqua");
        EssentiaContainers.Result filled = EssentiaContainers.fill(item, Aspect.ENERGY, 10);
        helper.assertTrue(filled != null && filled.amount() == 10 && EssentiaContainers.storedAmount(filled.stack()) == 10, "chamber item was not filled");
        EssentiaContainers.Result drained = EssentiaContainers.drain(filled.stack(), 10);
        helper.assertTrue(drained != null && drained.stack().is(VibrationFeature.CHAMBER.asItem()) && !AspectKeys.ASPECTS.has(drained.stack()), "chamber item was not drained");
        helper.succeed();
    }

    private static void producesPower(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:energy_cell")));
        helper.setBlock(CHAMBER, VibrationFeature.CHAMBER.get());
        VibrationChamberBlockEntity chamber = (VibrationChamberBlockEntity) helper.getBlockEntity(CHAMBER);
        chamber.setStored(Aspect.ENERGY, 2);
        helper.runAfterDelay(5, () -> PartHelper.setPart(helper.getLevel(), helper.absolutePos(CABLE), null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable"))));
        helper.runAfterDelay(80, () -> {
            IGrid grid = chamber.getMainNode().getGrid();
            double cell = ((appeng.blockentity.networking.EnergyCellBlockEntity) helper.getBlockEntity(ENERGY)).getAECurrentPower();
            helper.assertTrue(grid != null && cell > 0, "network got no power, cell " + cell + " ticks " + chamber.ticksRemaining());
            helper.assertTrue(chamber.storedAmount() == 1, "chamber stored " + chamber.storedAmount());
            helper.assertTrue(helper.getBlockState(CHAMBER).getValue(VibrationChamberBlock.PROCESSING) == VibrationChamberBlock.Processing.POTENTIA, "chamber face is not potentia");
            helper.succeed();
        });
    }
}
