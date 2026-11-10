package thaumicenergistics.test;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.storage.DriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.infusion.InfusionCore;
import thaumicenergistics.infusion.InfusionFeature;
import thaumicenergistics.infusion.InfusionAssemblerBlockEntity;
import thaumicenergistics.infusion.InfusionAssemblerPattern;
import thaumicenergistics.infusion.InfusionPattern;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.research.TeResearch;

public final class InfusionGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos ASSEMBLER = new BlockPos(4, 1, 1);

    private InfusionGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("infusion")
            .add("research_and_recipes", InfusionGameTests::researchAndRecipes)
            .add("pattern_inputs", InfusionGameTests::patternInputs)
            .add("crafting", InfusionGameTests::crafting)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static InfusionPattern shardPattern() {
        return new InfusionPattern(item("thaumcraft:air_shard"), java.util.List.of(item("thaumcraft:salis_mundus"), item("thaumcraft:salis_mundus")), item("thaumcraft:balanced_shard"),
            new AspectList().add(Aspect.FIRE, 2).add(Aspect.WATER, 2).add(Aspect.EARTH, 2).add(Aspect.ORDER, 2).add(Aspect.ENTROPY, 2));
    }

    private static void researchAndRecipes(GameTestHelper helper) {
        ResearchItem research = ResearchCategories.getResearch(TeResearch.INFUSION_ASSEMBLER);
        helper.assertTrue(research != null && research.parents().contains(TeResearch.CRUCIBLE_ASSEMBLER), "infusion assembler research is wrong");
        helper.assertTrue(TcRecipes.get(InfusionFeature.ASSEMBLER_RECIPE) != null, "assembler recipe is missing");
        helper.assertTrue(TcRecipes.get(InfusionFeature.CORE_RECIPE) != null, "core recipe is missing");
        helper.assertTrue(TcRecipes.get(InfusionFeature.INSCRIBER_RECIPE) != null, "inscriber recipe is missing");
        helper.succeed();
    }

    private static void patternInputs(GameTestHelper helper) {
        InfusionAssemblerPattern pattern = new InfusionAssemblerPattern(shardPattern());
        helper.assertTrue(pattern.getInputs().length == 7, "pattern has " + pattern.getInputs().length + " inputs");
        helper.assertTrue(pattern.getInputs()[0].getPossibleInputs()[0].what().equals(AEItemKey.of(item("thaumcraft:air_shard"))), "catalyst input is wrong");
        helper.assertTrue(pattern.getInputs()[1].getMultiplier() == 2, "components are not merged");
        var fire = pattern.getInputs()[2];
        helper.assertTrue(fire.getPossibleInputs()[0].what() instanceof EssentiaKey && fire.getMultiplier() == 2, "essentia input is wrong");
        helper.assertTrue(pattern.equals(PatternDetailsHelper.decodePattern(pattern.getDefinition(), helper.getLevel())), "pattern does not decode back");
        ItemStack core = new ItemStack(InfusionFeature.CORE.get());
        helper.assertTrue(InfusionCore.add(core, shardPattern()) && !InfusionCore.add(core, shardPattern()), "core accepts duplicates");
        helper.succeed();
    }

    private static void crafting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, item("ae2:item_storage_cell_4k"), false);
        helper.setBlock(ASSEMBLER, InfusionFeature.ASSEMBLER.get());
        InfusionAssemblerBlockEntity assembler = (InfusionAssemblerBlockEntity) helper.getBlockEntity(ASSEMBLER);
        ItemStack core = new ItemStack(InfusionFeature.CORE.get());
        InfusionCore.add(core, shardPattern());
        assembler.inventory().setItem(InfusionAssemblerBlockEntity.CORE_SLOT, core);
        helper.runAfterDelay(5, () -> PartHelper.setPart(helper.getLevel(), helper.absolutePos(CABLE), null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable"))));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(assembler.getMainNode().isActive(), "assembler is not active");
            IGrid grid = assembler.getMainNode().getGrid();
            helper.assertTrue(assembler.getAvailablePatterns().size() == 1, "assembler offers " + assembler.getAvailablePatterns().size() + " patterns");
            helper.assertTrue(grid.getCraftingService().isCraftable(AEItemKey.of(item("thaumcraft:balanced_shard"))), "shard is not craftable in the network");
            var pattern = assembler.getAvailablePatterns().get(0);
            helper.assertTrue(assembler.pushPattern(pattern, new KeyCounter[0]) && assembler.isBusy(), "pattern was not accepted");
            helper.assertTrue(!assembler.pushPattern(pattern, new KeyCounter[0]), "busy assembler accepted a pattern");
        });
        helper.runAfterDelay(60, () -> {
            IGrid grid = assembler.getMainNode().getGrid();
            long crafted = grid.getStorageService().getCachedInventory().get(AEItemKey.of(item("thaumcraft:balanced_shard")));
            helper.assertTrue(crafted == 1 && !assembler.isBusy(), "assembler crafted " + crafted);
            helper.succeed();
        });
    }
}
