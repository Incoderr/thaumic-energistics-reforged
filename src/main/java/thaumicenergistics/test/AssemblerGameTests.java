package thaumicenergistics.test;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.storage.DriveBlockEntity;
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
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.assembler.ArcaneAssemblerBlockEntity;
import thaumicenergistics.assembler.AssemblerFeature;
import thaumicenergistics.assembler.AssemblerPattern;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeCore;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.research.TeResearch;

public final class AssemblerGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos ASSEMBLER = new BlockPos(4, 1, 1);

    private AssemblerGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("assembler")
            .add("research_and_recipe", AssemblerGameTests::researchAndRecipe)
            .add("pattern_decoding", AssemblerGameTests::patternDecoding)
            .add("crafting", AssemblerGameTests::crafting)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ArcanePattern terminalPattern() {
        return new ArcanePattern(
            List.of(item("ae2:terminal"), item("thaumcraft:arcane_worktable"), item("ae2:calculation_processor")),
            new ItemStack(ActFeature.TERMINAL.get()),
            new AspectList().add(Aspect.AIR, 5).add(Aspect.ORDER, 10)
        );
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        helper.assertTrue(ResearchCategories.getResearch(TeResearch.ARCANE_ASSEMBLER).iconItem().item().toString().equals("thaumicenergistics:arcane_assembler"), "assembler research icon is wrong");
        InfusionRecipe recipe = (InfusionRecipe) TcRecipes.get(KnowledgeFeature.ASSEMBLER_RECIPE);
        helper.assertTrue(recipe != null && recipe.instability() == 7 && recipe.components().size() == 8, "assembler recipe is wrong");
        helper.assertTrue(recipe.components().get(0).test(Wands.create(WandRegistry.CAP_THAUMIUM, WandRegistry.ROD_SILVERWOOD, true)), "sceptre is not accepted");
        helper.assertTrue(!recipe.components().get(0).test(Wands.create(WandRegistry.CAP_THAUMIUM, WandRegistry.ROD_SILVERWOOD, false)), "plain wand is accepted");
        ItemStack result = recipe.result().create();
        AspectList vis = AssemblerFeature.STORED_VIS.get(result);
        helper.assertTrue(vis != null && vis.getAmount(Aspect.EARTH) == ArcaneAssemblerBlockEntity.MAX_STORED_CVIS, "crafted assembler holds no vis");
        helper.succeed();
    }

    private static void patternDecoding(GameTestHelper helper) {
        AssemblerPattern pattern = new AssemblerPattern(terminalPattern());
        helper.assertTrue(pattern.getInputs().length == 3 && pattern.getPrimaryOutput().what().equals(AEItemKey.of(ActFeature.TERMINAL.get())), "pattern inputs or output are wrong");
        var decoded = PatternDetailsHelper.decodePattern(pattern.getDefinition(), helper.getLevel());
        helper.assertTrue(pattern.equals(decoded), "pattern does not decode back");
        helper.succeed();
    }

    private static void crafting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, item("ae2:item_storage_cell_4k"), false);
        helper.setBlock(ASSEMBLER, AssemblerFeature.BLOCK.get());
        ArcaneAssemblerBlockEntity assembler = (ArcaneAssemblerBlockEntity) helper.getBlockEntity(ASSEMBLER);
        ItemStack core = new ItemStack(KnowledgeFeature.CORE.get());
        KnowledgeCore.add(core, terminalPattern());
        assembler.inventory().setItem(ArcaneAssemblerBlockEntity.KCORE_SLOT, core);
        assembler.setStoredVis(ArcaneAssemblerBlockEntity.fullVis());
        helper.runAfterDelay(5, () -> PartHelper.setPart(helper.getLevel(), helper.absolutePos(CABLE), null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable"))));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(assembler.getMainNode().isActive(), "assembler is not active");
            IGrid grid = assembler.getMainNode().getGrid();
            helper.assertTrue(assembler.getAvailablePatterns().size() == 1, "assembler offers " + assembler.getAvailablePatterns().size() + " patterns");
            helper.assertTrue(grid.getCraftingService().isCraftable(AEItemKey.of(ActFeature.TERMINAL.get())), "terminal is not craftable in the network");
            var pattern = assembler.getAvailablePatterns().get(0);
            helper.assertTrue(assembler.pushPattern(pattern, new KeyCounter[0]) && assembler.isBusy(), "pattern was not accepted");
            helper.assertTrue(!assembler.pushPattern(pattern, new KeyCounter[0]), "busy assembler accepted a pattern");
        });
        helper.runAfterDelay(60, () -> {
            IGrid grid = assembler.getMainNode().getGrid();
            long crafted = grid.getStorageService().getCachedInventory().get(AEItemKey.of(ActFeature.TERMINAL.get()));
            helper.assertTrue(crafted == 1 && !assembler.isBusy(), "assembler crafted " + crafted);
            int order = assembler.storedVis().getAmount(Aspect.ORDER);
            helper.assertTrue(order == ArcaneAssemblerBlockEntity.MAX_STORED_CVIS - 80, "order vis left is " + order);
            helper.assertTrue(assembler.storedVis().getAmount(Aspect.FIRE) == ArcaneAssemblerBlockEntity.MAX_STORED_CVIS, "unused vis was spent");
            helper.succeed();
        });
    }
}
