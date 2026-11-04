package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.parts.AEBasePart;
import java.util.List;
import java.util.function.BiConsumer;
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
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.buses.BusesFeature;
import thaumicenergistics.buses.EssentiaBusMenu;
import thaumicenergistics.buses.EssentiaExportBusPart;
import thaumicenergistics.buses.EssentiaImportBusPart;
import thaumicenergistics.buses.EssentiaStorageBusPart;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaTileContainers;

public final class BusesGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos TARGET = new BlockPos(4, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private BusesGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("buses")
            .add("research_and_recipes", BusesGameTests::researchAndRecipes)
            .add("tile_containers", BusesGameTests::tileContainers)
            .add("import_bus", BusesGameTests::importBus)
            .add("export_bus", BusesGameTests::exportBus)
            .add("export_bus_void", BusesGameTests::exportBusVoid)
            .add("storage_bus", BusesGameTests::storageBus)
            .add("bus_menu", BusesGameTests::busMenu)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ItemStack phial(Aspect aspect) {
        ItemStack stack = item("thaumcraft:essentia_phial");
        AspectKeys.ASPECTS.set(stack, new AspectList().add(aspect, 8));
        return stack;
    }

    private static JarBlockEntity jar(GameTestHelper helper, String block, Aspect aspect, int amount) {
        helper.setBlock(TARGET, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)));
        JarBlockEntity jar = (JarBlockEntity) helper.getBlockEntity(TARGET);
        if (aspect != null) {
            jar.setAspect(aspect);
            jar.setAmount(amount);
        }
        return jar;
    }

    private static long stored(MEStorage storage, Aspect aspect) {
        return storage.extract(EssentiaKey.of(aspect), Long.MAX_VALUE, Actionable.SIMULATE, SOURCE);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AEBasePart> void network(GameTestHelper helper, IPartItem<T> busItem, BiConsumer<T, IGrid> body) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get()), false);
        BlockPos cable = helper.absolutePos(CABLE);
        PartHelper.setPart(helper.getLevel(), cable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
        T part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, busItem);
        helper.assertTrue(part != null, "bus part was not placed");
        helper.runAfterDelay(20, () -> {
            IGrid grid = part.getMainNode().getGrid();
            helper.assertTrue(grid != null && part.getMainNode().isActive(), "bus is not active in the network");
            body.accept(part, grid);
            helper.succeed();
        });
    }

    private static void researchAndRecipes(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch("thaumicenergistics.TEIO");
        helper.assertTrue(item != null, "IO research is missing");
        helper.assertTrue(item.column() == -2 && item.row() == -2 && item.complexity() == 2 && item.isConcealed(), "IO research flags are wrong");
        helper.assertTrue(item.parents().equals(List.of("thaumicenergistics.TECORES", "thaumicenergistics.Pseudo.TUBEFILTER")), "IO research parents are " + item.parents());
        helper.assertTrue(item.parentsHidden().equals(List.of("TUBEFILTER")), "IO hidden parents are " + item.parentsHidden());
        helper.assertTrue(item.pages().size() == 5, "IO research has " + item.pages().size() + " pages");
        for (String key : List.of(BusesFeature.IMPORT_BUS_RECIPE, BusesFeature.EXPORT_BUS_RECIPE, BusesFeature.STORAGE_BUS_RECIPE)) {
            helper.assertTrue(TcRecipes.get(key) != null, "recipe " + key + " is missing");
        }
        helper.succeed();
    }

    private static void tileContainers(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, "thaumcraft:warded_jar", Aspect.FIRE, 60);
        helper.assertTrue(EssentiaTileContainers.inject(jar, 10, Aspect.WATER, false) == 0, "jar accepted another aspect");
        helper.assertTrue(EssentiaTileContainers.inject(jar, 10, Aspect.FIRE, true) == 4, "jar overfilled past 64");
        helper.assertTrue(jar.getAmount() == 64, "jar amount is " + jar.getAmount());
        helper.assertTrue(EssentiaTileContainers.extract(jar, 100, Aspect.FIRE, true) == 64 && jar.getAmount() == 0, "jar was not drained");
        jar.setAspectFilter(Aspect.ORDER);
        helper.assertTrue(EssentiaTileContainers.inject(jar, 5, Aspect.FIRE, false) == 0, "labelled jar accepted another aspect");
        helper.assertTrue(EssentiaTileContainers.inject(jar, 5, Aspect.ORDER, false) == 5, "labelled jar refused its aspect");
        helper.succeed();
    }

    private static void importBus(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, "thaumcraft:warded_jar", Aspect.FIRE, 30);
        network(helper, BusesFeature.IMPORT_BUS.get(), (EssentiaImportBusPart part, IGrid grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            part.setFilter(4, Aspect.WATER);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(jar.getAmount() == 30, "filtered import bus moved fire");
            part.setFilter(4, null);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(jar.getAmount() == 26 && stored(storage, Aspect.FIRE) == 4, "import moved " + (30 - jar.getAmount()));
            part.getUpgrades().addItems(new ItemStack(AEItems.SPEED_CARD));
            helper.assertTrue(part.transferAmountPerSecond() == 12, "speed card does not add 8 per second");
            part.tickingRequest(part.getGridNode(), 40);
            helper.assertTrue(jar.getAmount() == 2 && stored(storage, Aspect.FIRE) == 28, "fast import moved " + (26 - jar.getAmount()));
        });
    }

    private static void exportBus(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, "thaumcraft:warded_jar", null, 0);
        network(helper, BusesFeature.EXPORT_BUS.get(), (EssentiaExportBusPart part, IGrid grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            storage.insert(EssentiaKey.of(Aspect.WATER), 20, Actionable.MODULATE, SOURCE);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(jar.getAmount() == 0, "unfiltered export bus moved essentia");
            part.setFilter(4, Aspect.WATER);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(jar.getAspect() == Aspect.WATER && jar.getAmount() == 4 && stored(storage, Aspect.WATER) == 16, "export moved " + jar.getAmount());
        });
    }

    private static void exportBusVoid(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, "thaumcraft:void_jar", Aspect.WATER, 64);
        network(helper, BusesFeature.EXPORT_BUS.get(), (EssentiaExportBusPart part, IGrid grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            storage.insert(EssentiaKey.of(Aspect.WATER), 20, Actionable.MODULATE, SOURCE);
            part.setFilter(4, Aspect.WATER);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(stored(storage, Aspect.WATER) == 20, "full void jar took essentia without void mode");
            part.setVoidAllowed(true);
            part.tickingRequest(part.getGridNode(), 20);
            helper.assertTrue(stored(storage, Aspect.WATER) == 16 && jar.getAmount() == 64, "void mode did not void 4 water");
        });
    }

    private static void storageBus(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, "thaumcraft:warded_jar", Aspect.FIRE, 30);
        network(helper, BusesFeature.STORAGE_BUS.get(), (EssentiaStorageBusPart part, IGrid grid) -> {
            part.updateTarget();
            helper.assertTrue(part.connectedContainer() == jar, "storage bus did not find the jar");
            MEStorage storage = grid.getStorageService().getInventory();
            KeyCounter available = storage.getAvailableStacks();
            helper.assertTrue(available.get(EssentiaKey.of(Aspect.FIRE)) == 30, "network does not see jar fire");
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.FIRE), 10, Actionable.MODULATE, SOURCE) == 10 && jar.getAmount() == 20, "storage bus extract is wrong");
            part.setPriority(10);
            helper.assertTrue(storage.insert(EssentiaKey.of(Aspect.FIRE), 5, Actionable.MODULATE, SOURCE) == 5 && jar.getAmount() == 25, "prioritised storage bus did not take fire");
            helper.assertTrue(storage.insert(EssentiaKey.of(Aspect.WATER), 5, Actionable.MODULATE, SOURCE) == 5 && jar.getAmount() == 25, "jar took water over fire");
            part.setFilter(0, Aspect.WATER);
            helper.assertTrue(storage.getAvailableStacks().get(EssentiaKey.of(Aspect.FIRE)) == 0, "filtered storage bus still shows fire");
            part.getUpgrades().addItems(new ItemStack(AEItems.INVERTER_CARD));
            helper.assertTrue(part.isInverted(), "inverter card is not installed");
            part.setFilter(0, null);
            helper.assertTrue(part.allowAny(), "filters were not cleared");
        });
    }

    private static void busMenu(GameTestHelper helper) {
        network(helper, BusesFeature.IMPORT_BUS.get(), (EssentiaImportBusPart part, IGrid grid) -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            EssentiaBusMenu menu = new EssentiaBusMenu(1, player.getInventory(), part);
            helper.assertTrue(part.availableFilterSlots().length == 1 && part.availableFilterSlots()[0] == 4, "base import bus has more than the center filter");
            menu.setCarried(phial(Aspect.AIR));
            menu.clickFilter(0);
            helper.assertTrue(part.getFilter(0) == null, "hidden filter slot was set");
            menu.clickFilter(4);
            helper.assertTrue(part.getFilter(4) == Aspect.AIR, "filter slot was not set from the held phial");
            menu.setCarried(ItemStack.EMPTY);
            part.getUpgrades().addItems(new ItemStack(AEItems.CAPACITY_CARD));
            helper.assertTrue(part.availableFilterSlots().length == 5, "capacity card did not open tier 1 filters");
            player.getInventory().setItem(9, phial(Aspect.EARTH));
            int slot = -1;
            for (int index = 0; index < menu.slots.size(); index++) {
                if (menu.slots.get(index).container == player.getInventory() && menu.slots.get(index).getContainerSlot() == 9) {
                    slot = index;
                }
            }
            menu.quickMoveStack(player, slot);
            helper.assertTrue(part.getFilter(1) == Aspect.EARTH, "shift click did not add a filter");
            helper.assertTrue(player.getInventory().getItem(9).getCount() == 1, "shift click moved the phial");
            part.getUpgrades().extractItem(0, 1, false);
            helper.assertTrue(part.getFilter(1) == null && part.getFilter(4) == Aspect.AIR, "removing capacity did not clear tier 1 filters");
            menu.clickFilter(4);
            helper.assertTrue(part.getFilter(4) == null, "empty hand did not clear the filter");
        });
    }
}
