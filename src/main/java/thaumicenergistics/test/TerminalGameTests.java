package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import java.util.List;
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
import thaumcraft.essentia.EssentiaKeys;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.terminal.EssentiaListPayload;
import thaumicenergistics.terminal.EssentiaTerminalMenu;
import thaumicenergistics.terminal.EssentiaTerminalPart;
import thaumicenergistics.terminal.TerminalFeature;

public final class TerminalGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private TerminalGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("terminal")
            .add("phial_fill_and_drain", TerminalGameTests::phialFillAndDrain)
            .add("jar_fill_and_drain", TerminalGameTests::jarFillAndDrain)
            .add("label", TerminalGameTests::label)
            .add("research", TerminalGameTests::research)
            .add("terminal_in_network", TerminalGameTests::terminalInNetwork)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ItemStack phial(Aspect aspect) {
        ItemStack stack = item("thaumcraft:essentia_phial");
        AspectKeys.ASPECTS.set(stack, new AspectList().add(aspect, EssentiaContainers.PHIAL_CAPACITY));
        return stack;
    }

    private static void phialFillAndDrain(GameTestHelper helper) {
        ItemStack glass = item("thaumcraft:glass_phial");
        helper.assertTrue(EssentiaContainers.fill(glass, Aspect.FIRE, 7) == null, "phial filled with less than 8");
        EssentiaContainers.Result filled = EssentiaContainers.fill(glass, Aspect.FIRE, 20);
        helper.assertTrue(filled != null && filled.amount() == 8 && EssentiaContainers.storedAspect(filled.stack()) == Aspect.FIRE, "phial fill is wrong");
        helper.assertTrue(EssentiaContainers.drain(phial(Aspect.FIRE), 7) == null, "phial drained partially");
        EssentiaContainers.Result drained = EssentiaContainers.drain(phial(Aspect.FIRE), 8);
        helper.assertTrue(drained != null && drained.amount() == 8 && drained.stack().is(glass.getItem()), "phial drain is wrong");
        helper.succeed();
    }

    private static void jarFillAndDrain(GameTestHelper helper) {
        ItemStack jar = item("thaumcraft:filled_jar");
        AspectKeys.ASPECTS.set(jar, new AspectList().add(Aspect.WATER, 30));
        EssentiaContainers.Result partial = EssentiaContainers.drain(jar, 10);
        helper.assertTrue(partial != null && partial.amount() == 10 && EssentiaContainers.storedAmount(partial.stack()) == 20, "partial jar drain is wrong");
        EssentiaContainers.Result full = EssentiaContainers.drain(jar, 64);
        helper.assertTrue(full != null && full.amount() == 30 && full.stack().is(item("thaumcraft:warded_jar").getItem()), "full jar drain is wrong: " + (full == null ? null : full.stack()));
        ItemStack labelled = item("thaumcraft:filled_jar");
        EssentiaKeys.FILTER.set(labelled, Aspect.WATER);
        helper.assertTrue(EssentiaContainers.fill(labelled, Aspect.FIRE, 10) == null, "labelled jar took another aspect");
        EssentiaContainers.Result filled = EssentiaContainers.fill(labelled, Aspect.WATER, 100);
        helper.assertTrue(filled != null && filled.amount() == 64, "labelled jar fill is wrong");
        ItemStack labelledFull = filled.stack();
        EssentiaContainers.Result emptied = EssentiaContainers.drain(labelledFull, 64);
        helper.assertTrue(emptied != null && EssentiaKeys.FILTER.get(emptied.stack()) == Aspect.WATER && EssentiaContainers.storedAmount(emptied.stack()) == 0, "labelled jar lost its label");
        helper.succeed();
    }

    private static void label(GameTestHelper helper) {
        ItemStack label = item("thaumcraft:jar_label");
        helper.assertTrue(EssentiaContainers.isLabel(label), "label is not recognised");
        ItemStack set = EssentiaContainers.withLabel(label, Aspect.ORDER);
        helper.assertTrue(AspectKeys.ASPECTS.get(set).getAmount(Aspect.ORDER) == 1, "label aspect not set");
        helper.assertTrue(!AspectKeys.ASPECTS.has(EssentiaContainers.withLabel(set, null)), "label was not cleared");
        helper.succeed();
    }

    private static void research(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch("thaumicenergistics.TEESSTERM");
        helper.assertTrue(item != null, "essentia terminal research is missing");
        helper.assertTrue(item.column() == -1 && item.row() == -4 && item.isConcealed() && item.isSecondary(), "research flags are wrong");
        helper.assertTrue(item.parents().equals(List.of("thaumicenergistics.TECORES")), "research parents are " + item.parents());
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void terminalInNetwork(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get()), false);
        BlockPos cable = helper.absolutePos(CABLE);
        PartHelper.setPart(helper.getLevel(), cable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
        EssentiaTerminalPart part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, TerminalFeature.TERMINAL.get());
        helper.assertTrue(part != null, "terminal part was not placed");
        helper.runAfterDelay(20, () -> {
            IGrid grid = part.getEssentiaGrid();
            helper.assertTrue(grid != null, "terminal is not active in the network");
            MEStorage storage = grid.getStorageService().getInventory();
            helper.assertTrue(storage.insert(EssentiaKey.of(Aspect.FIRE), 20, Actionable.MODULATE, SOURCE) == 20, "network did not accept essentia");
            EssentiaTerminalMenu menu = new EssentiaTerminalMenu(1, player.getInventory(), part);

            part.setSelectedAspect(Aspect.FIRE);
            part.getWorkInventory().setItemDirect(0, item("thaumcraft:glass_phial").copyWithCount(3));
            menu.transferFromWorkSlots();
            ItemStack output = part.getWorkInventory().getStackInSlot(1);
            helper.assertTrue(output.getCount() == 2 && EssentiaContainers.storedAspect(output) == Aspect.FIRE, "filled phials are " + output);
            helper.assertTrue(part.getWorkInventory().getStackInSlot(0).getCount() == 1, "unfilled phial was consumed");
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.FIRE), Long.MAX_VALUE, Actionable.SIMULATE, SOURCE) == 4, "network fire is wrong after filling");

            part.getWorkInventory().setItemDirect(0, phial(Aspect.WATER));
            part.getWorkInventory().setItemDirect(1, ItemStack.EMPTY);
            menu.transferFromWorkSlots();
            helper.assertTrue(part.getWorkInventory().getStackInSlot(1).is(item("thaumcraft:glass_phial").getItem()), "phial was not drained");
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.WATER), Long.MAX_VALUE, Actionable.SIMULATE, SOURCE) == 8, "network water is wrong after draining");

            part.getWorkInventory().setItemDirect(0, item("thaumcraft:jar_label"));
            part.getWorkInventory().setItemDirect(1, ItemStack.EMPTY);
            menu.transferFromWorkSlots();
            helper.assertTrue(AspectKeys.ASPECTS.get(part.getWorkInventory().getStackInSlot(1)).getAmount(Aspect.FIRE) == 1, "label was not set");

            menu.setCarried(phial(Aspect.ORDER));
            menu.interactWithHeldItem(Aspect.FIRE.getTag());
            helper.assertTrue(menu.getCarried().is(item("thaumcraft:glass_phial").getItem()), "held phial was not drained: " + menu.getCarried());
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.ORDER), Long.MAX_VALUE, Actionable.SIMULATE, SOURCE) == 8, "network order is wrong after held drain");

            List<EssentiaListPayload.Entry> entries = EssentiaTerminalMenu.collectEntries(grid);
            helper.assertTrue(entries.contains(new EssentiaListPayload.Entry(Aspect.FIRE, 4, false)), "essentia list is " + entries);
            helper.succeed();
        });
    }
}
