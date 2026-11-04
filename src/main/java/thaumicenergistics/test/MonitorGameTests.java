package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.parts.AEBasePart;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectKeys;
import thaumcraft.aspect.AspectList;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.monitor.EssentiaConversionMonitorPart;
import thaumicenergistics.monitor.EssentiaStorageMonitorPart;
import thaumicenergistics.monitor.MonitorFeature;
import thaumicenergistics.terminal.TerminalFeature;

public final class MonitorGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private MonitorGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("monitor")
            .add("recipes_and_research", MonitorGameTests::recipesAndResearch)
            .add("storage_monitor", MonitorGameTests::storageMonitor)
            .add("conversion_monitor", MonitorGameTests::conversionMonitor)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ItemStack filled(String id, Aspect aspect, int amount) {
        ItemStack stack = item(id);
        AspectKeys.ASPECTS.set(stack, new AspectList().add(aspect, amount));
        return stack;
    }

    private static long stored(MEStorage storage, Aspect aspect) {
        return storage.extract(EssentiaKey.of(aspect), Long.MAX_VALUE, Actionable.SIMULATE, SOURCE);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AEBasePart> void network(GameTestHelper helper, IPartItem<T> partItem, BiConsumer<T, IGrid> body) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get()), false);
        BlockPos cable = helper.absolutePos(CABLE);
        PartHelper.setPart(helper.getLevel(), cable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
        T part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, partItem);
        helper.assertTrue(part != null, "monitor was not placed");
        helper.runAfterDelay(20, () -> {
            IGrid grid = part.getMainNode().getGrid();
            helper.assertTrue(grid != null && part.isActive(), "monitor is not active in the network");
            body.accept(part, grid);
        });
    }

    private static void recipesAndResearch(GameTestHelper helper) {
        for (String name : new String[] {"essentia_storage_monitor", "essentia_conversion_monitor"}) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ThaumicEnergistics.id(name)).isPresent(), "recipe " + name + " is not loaded");
        }
        var pages = ResearchCategories.getResearch("thaumicenergistics.TEESSTERM").pages();
        helper.assertTrue(pages.contains(ResearchPage.crafting(TerminalFeature.STORAGE_MONITOR_RECIPE)) && pages.contains(ResearchPage.crafting(TerminalFeature.CONVERSION_MONITOR_RECIPE)), "terminal research lacks monitor pages");
        helper.assertTrue(pages.size() == 9, "terminal research has " + pages.size() + " pages");
        helper.succeed();
    }

    private static void storageMonitor(GameTestHelper helper) {
        network(helper, MonitorFeature.STORAGE_MONITOR.get(), (EssentiaStorageMonitorPart part, IGrid grid) -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            grid.getStorageService().getInventory().insert(EssentiaKey.of(Aspect.FIRE), 20, Actionable.MODULATE, SOURCE);
            ItemStack phial = filled("thaumcraft:essentia_phial", Aspect.FIRE, 8);
            helper.assertTrue(part.onUseItemOn(phial, player, InteractionHand.MAIN_HAND, Vec3.ZERO), "phial did not set the monitor");
            helper.assertTrue(part.getTrackedAspect() == Aspect.FIRE, "monitor tracks " + part.getTrackedAspect());
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(part.getTrackedAmount() == 20, "monitor amount is " + part.getTrackedAmount());
                grid.getStorageService().getInventory().insert(EssentiaKey.of(Aspect.FIRE), 5, Actionable.MODULATE, SOURCE);
            });
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(part.getTrackedAmount() == 25, "monitor amount did not update: " + part.getTrackedAmount());
                helper.assertTrue(part.onUseItemOn(item("ae2:certus_quartz_wrench"), player, InteractionHand.MAIN_HAND, Vec3.ZERO) && part.isLocked(), "wrench did not lock");
                helper.assertTrue(!part.onUseWithoutItem(player, Vec3.ZERO) && part.getTrackedAspect() == Aspect.FIRE, "locked monitor was cleared");
                part.onUseItemOn(item("ae2:certus_quartz_wrench"), player, InteractionHand.MAIN_HAND, Vec3.ZERO);
                helper.assertTrue(part.onUseWithoutItem(player, Vec3.ZERO) && part.getTrackedAspect() == null, "empty hand did not clear");
                helper.succeed();
            });
        });
    }

    private static void conversionMonitor(GameTestHelper helper) {
        network(helper, MonitorFeature.CONVERSION_MONITOR.get(), (EssentiaConversionMonitorPart part, IGrid grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().selected = 0;
            player.setItemInHand(InteractionHand.MAIN_HAND, filled("thaumcraft:filled_jar", Aspect.WATER, 30));
            player.getInventory().setItem(5, filled("thaumcraft:essentia_phial", Aspect.WATER, 8).copyWithCount(2));
            player.getInventory().setItem(6, filled("thaumcraft:essentia_phial", Aspect.FIRE, 8));
            player.setShiftKeyDown(true);
            helper.assertTrue(part.onUseItemOn(player.getMainHandItem(), player, InteractionHand.MAIN_HAND, Vec3.ZERO), "shift click did not drain");
            helper.assertTrue(stored(storage, Aspect.WATER) == 30 && player.getMainHandItem().is(item("thaumcraft:warded_jar").getItem()), "jar drain is wrong: " + player.getMainHandItem());
            player.setItemInHand(InteractionHand.MAIN_HAND, filled("thaumcraft:essentia_phial", Aspect.WATER, 8));
            helper.assertTrue(part.onUseItemOn(player.getMainHandItem(), player, InteractionHand.MAIN_HAND, Vec3.ZERO), "double click failed");
            helper.assertTrue(stored(storage, Aspect.WATER) == 54, "double click deposited " + (stored(storage, Aspect.WATER) - 30));
            helper.assertTrue(stored(storage, Aspect.FIRE) == 0, "double click deposited another aspect");
            player.setShiftKeyDown(false);
            part.onUseItemOn(filled("thaumcraft:essentia_phial", Aspect.WATER, 8), player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            part.onUseItemOn(item("ae2:certus_quartz_wrench"), player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(part.getTrackedAspect() == Aspect.WATER && part.isLocked(), "monitor is not locked on water");
            player.setItemInHand(InteractionHand.MAIN_HAND, item("thaumcraft:warded_jar"));
            helper.assertTrue(part.onUseItemOn(player.getMainHandItem(), player, InteractionHand.MAIN_HAND, Vec3.ZERO), "fill click failed");
            helper.assertTrue(EssentiaContainers.storedAmount(player.getMainHandItem()) == 54 && stored(storage, Aspect.WATER) == 0, "jar fill is wrong: " + player.getMainHandItem());
            ItemStack label = item("thaumcraft:jar_label");
            player.setItemInHand(InteractionHand.MAIN_HAND, label);
            part.onUseItemOn(label, player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(AspectKeys.ASPECTS.get(player.getMainHandItem()).getAmount(Aspect.WATER) == 1, "label was not set");
            helper.succeed();
        });
    }
}
