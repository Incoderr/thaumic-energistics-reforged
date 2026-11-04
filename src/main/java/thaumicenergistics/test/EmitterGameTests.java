package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.blockentity.storage.DriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.emitter.EmitterFeature;
import thaumicenergistics.emitter.EssentiaLevelEmitterMenu;
import thaumicenergistics.emitter.EssentiaLevelEmitterPart;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.terminal.TerminalFeature;

public final class EmitterGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private EmitterGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("emitter")
            .add("recipe_and_research", EmitterGameTests::recipeAndResearch)
            .add("emits_on_level", EmitterGameTests::emitsOnLevel)
            .add("menu", EmitterGameTests::menu)
            .register(bus);
    }

    private static void recipeAndResearch(GameTestHelper helper) {
        helper.assertTrue(TcRecipes.get(TerminalFeature.LEVEL_EMITTER_RECIPE) != null, "level emitter recipe is missing");
        helper.assertTrue(ResearchCategories.getResearch("thaumicenergistics.TEESSTERM").pages().contains(ResearchPage.arcane(TerminalFeature.LEVEL_EMITTER_RECIPE)), "terminal research has no level emitter page");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static EssentiaLevelEmitterPart emitter(GameTestHelper helper, Player player) {
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get()), false);
        BlockPos cable = helper.absolutePos(CABLE);
        PartHelper.setPart(helper.getLevel(), cable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
        EssentiaLevelEmitterPart part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, EmitterFeature.LEVEL_EMITTER.get());
        helper.assertTrue(part != null, "level emitter was not placed");
        return part;
    }

    private static void emitsOnLevel(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EssentiaLevelEmitterPart part = emitter(helper, player);
        helper.runAfterDelay(20, () -> {
            IGrid grid = part.getMainNode().getGrid();
            helper.assertTrue(grid != null && part.getMainNode().isActive(), "level emitter is not active");
            part.setTrackedAspect(Aspect.FIRE);
            part.setThresholdLevel(10);
            grid.getStorageService().getInventory().insert(EssentiaKey.of(Aspect.FIRE), 5, Actionable.MODULATE, SOURCE);
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(part.getCurrentLevel() == 5 && !part.isEmitting(), "emitter is on below the threshold");
                grid.getStorageService().getInventory().insert(EssentiaKey.of(Aspect.FIRE), 5, Actionable.MODULATE, SOURCE);
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(part.getCurrentLevel() == 10 && part.isEmitting() && part.isProvidingStrongPower() == 15, "emitter is off at the threshold");
                    part.getConfigManager().putSetting(Settings.REDSTONE_EMITTER, RedstoneMode.LOW_SIGNAL);
                    helper.assertTrue(!part.isEmitting(), "low signal mode still emits at the threshold");
                    part.setThresholdLevel(11);
                    helper.assertTrue(part.isEmitting(), "low signal mode does not emit below the threshold");
                    helper.succeed();
                });
            });
        });
    }

    private static void menu(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EssentiaLevelEmitterPart part = emitter(helper, player);
        helper.runAfterDelay(20, () -> {
            EssentiaLevelEmitterMenu menu = new EssentiaLevelEmitterMenu(1, player.getInventory(), part);
            menu.adjustThreshold(100);
            menu.adjustThreshold(7);
            helper.assertTrue(part.getThresholdLevel() == 100, "threshold after adjust is " + part.getThresholdLevel());
            menu.adjustThreshold(-1000);
            helper.assertTrue(part.getThresholdLevel() == 0, "threshold went below zero");
            menu.setThreshold(Long.MAX_VALUE);
            helper.assertTrue(part.getThresholdLevel() == EssentiaLevelEmitterPart.MAX_THRESHOLD, "threshold is not clamped");
            ItemStack phial = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:essentia_phial")));
            AspectKeys.ASPECTS.set(phial, new AspectList().add(Aspect.ORDER, 8));
            menu.setCarried(phial);
            menu.clickAspect(true);
            helper.assertTrue(part.getTrackedAspect() == Aspect.ORDER, "aspect was not set from the held phial");
            menu.setCarried(ItemStack.EMPTY);
            menu.clickAspect(true);
            helper.assertTrue(part.getTrackedAspect() == null, "empty hand did not clear the aspect");
            helper.succeed();
        });
    }
}
