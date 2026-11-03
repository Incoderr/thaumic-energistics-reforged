package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.CellState;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaCellInventory;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaKeyType;

public final class CellsGameTests {
    private static final IActionSource SOURCE = IActionSource.empty();

    private CellsGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("cells")
            .add("key_type_registered", CellsGameTests::keyTypeRegistered)
            .add("key_tag_roundtrip", CellsGameTests::keyTagRoundtrip)
            .add("cell_handler_registered", CellsGameTests::cellHandlerRegistered)
            .add("capacity_matches_te", CellsGameTests::capacityMatchesTe)
            .add("type_limit", CellsGameTests::typeLimit)
            .add("used_bytes_round_up", CellsGameTests::usedBytesRoundUp)
            .add("extract", CellsGameTests::extract)
            .add("partition", CellsGameTests::partition)
            .add("persisted_to_stack", CellsGameTests::persistedToStack)
            .add("rejects_items", CellsGameTests::rejectsItems)
            .add("creative", CellsGameTests::creative)
            .add("disassemble_empty_cell", CellsGameTests::disassembleEmptyCell)
            .add("full_cell_not_disassembled", CellsGameTests::fullCellNotDisassembled)
            .register(bus);
    }

    private static EssentiaCellInventory cell(EssentiaStorageType type) {
        return EssentiaCellInventory.of(new ItemStack(CellsFeature.cell(type).get()), null);
    }

    private static AEKey key(Aspect aspect) {
        return EssentiaKey.of(aspect);
    }

    private static void keyTypeRegistered(GameTestHelper helper) {
        helper.assertTrue(AEKeyTypes.get(ThaumicEnergistics.id("essentia")) == EssentiaKeyType.TYPE, "essentia key type is not registered");
        helper.assertTrue(EssentiaKeyType.TYPE.getAmountPerByte() == 2, "essentia per byte is not 2");
        helper.assertTrue(EssentiaKey.of(Aspect.FIRE) == EssentiaKey.of(Aspect.FIRE), "essentia keys are not cached");
        helper.succeed();
    }

    private static void keyTagRoundtrip(GameTestHelper helper) {
        CompoundTag tag = EssentiaKey.of(Aspect.MAGIC).toTagGeneric(helper.getLevel().registryAccess());
        AEKey loaded = AEKey.fromTagGeneric(helper.getLevel().registryAccess(), tag);
        helper.assertTrue(EssentiaKey.of(Aspect.MAGIC).equals(loaded), "essentia key does not survive tag roundtrip: " + loaded);
        helper.succeed();
    }

    private static void cellHandlerRegistered(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_1K).get());
        helper.assertTrue(StorageCells.getCellInventory(stack, null) instanceof EssentiaCellInventory, "essentia cell handler is not registered");
        helper.assertTrue(StorageCells.getCellInventory(new ItemStack(CellsFeature.HOUSING.get()), null) == null, "housing is treated as a cell");
        helper.succeed();
    }

    private static void capacityMatchesTe(GameTestHelper helper) {
        long[] bytes = {1024, 4096, 16348, 65536};
        EssentiaStorageType[] types = {EssentiaStorageType.TYPE_1K, EssentiaStorageType.TYPE_4K, EssentiaStorageType.TYPE_16K, EssentiaStorageType.TYPE_64K};
        for (int index = 0; index < types.length; index++) {
            EssentiaCellInventory cell = cell(types[index]);
            long inserted = cell.insert(key(Aspect.FIRE), Long.MAX_VALUE, Actionable.MODULATE, SOURCE);
            helper.assertTrue(inserted == bytes[index] * 2, types[index] + " stores " + inserted + " essentia");
            helper.assertTrue(cell.getUsedBytes() == bytes[index] && cell.getTotalBytes() == bytes[index], types[index] + " byte counters are wrong");
            helper.assertTrue(cell.getStatus() == CellState.FULL, types[index] + " is not reported full");
            helper.assertTrue(cell.insert(key(Aspect.WATER), 1, Actionable.MODULATE, SOURCE) == 0, types[index] + " accepts essentia when full");
        }
        helper.succeed();
    }

    private static void typeLimit(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.TYPE_1K);
        Aspect[] aspects = Aspect.values();
        for (int index = 0; index < 12; index++) {
            helper.assertTrue(cell.insert(key(aspects[index]), 1, Actionable.MODULATE, SOURCE) == 1, "type " + index + " rejected");
        }
        helper.assertTrue(cell.insert(key(aspects[12]), 1, Actionable.MODULATE, SOURCE) == 0, "13th type accepted");
        helper.assertTrue(cell.insert(key(aspects[0]), 5, Actionable.MODULATE, SOURCE) == 5, "existing type rejected when types are full");
        helper.assertTrue(cell.getStatus() == CellState.TYPES_FULL, "cell is not reported types full");
        helper.assertTrue(cell.getUsedTypes() == 12 && cell.getTotalTypes() == 12, "type counters are wrong");
        helper.succeed();
    }

    private static void usedBytesRoundUp(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.TYPE_1K);
        cell.insert(key(Aspect.FIRE), 3, Actionable.MODULATE, SOURCE);
        helper.assertTrue(cell.getUsedBytes() == 2, "3 essentia use " + cell.getUsedBytes() + " bytes");
        helper.assertTrue(cell.getFreeBytes() == 1022, "free bytes are " + cell.getFreeBytes());
        helper.succeed();
    }

    private static void extract(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.TYPE_1K);
        cell.insert(key(Aspect.FIRE), 10, Actionable.MODULATE, SOURCE);
        helper.assertTrue(cell.extract(key(Aspect.FIRE), 4, Actionable.SIMULATE, SOURCE) == 4, "simulated extract is wrong");
        helper.assertTrue(cell.getStoredEssentia().get(Aspect.FIRE) == 10, "simulated extract changed the cell");
        helper.assertTrue(cell.extract(key(Aspect.FIRE), 4, Actionable.MODULATE, SOURCE) == 4, "extract is wrong");
        helper.assertTrue(cell.getStoredEssentia().get(Aspect.FIRE) == 6, "extract left the wrong amount");
        helper.assertTrue(cell.extract(key(Aspect.FIRE), 100, Actionable.MODULATE, SOURCE) == 6, "extract more than stored is wrong");
        helper.assertTrue(cell.getUsedTypes() == 0 && cell.getStatus() == CellState.EMPTY, "empty type was not removed");
        helper.assertTrue(cell.extract(key(Aspect.WATER), 1, Actionable.MODULATE, SOURCE) == 0, "extracted a missing aspect");
        helper.succeed();
    }

    private static void partition(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.TYPE_1K);
        cell.addToPartition(Aspect.FIRE);
        helper.assertTrue(cell.isPartitioned(), "cell is not partitioned");
        helper.assertTrue(cell.insert(key(Aspect.WATER), 5, Actionable.MODULATE, SOURCE) == 0, "partitioned cell accepted another aspect");
        helper.assertTrue(cell.insert(key(Aspect.FIRE), 5, Actionable.MODULATE, SOURCE) == 5, "partitioned cell rejected its aspect");
        helper.assertTrue(cell.isPreferredStorageFor(key(Aspect.FIRE), SOURCE), "partitioned aspect is not preferred");
        helper.assertTrue(!cell.isPreferredStorageFor(key(Aspect.WATER), SOURCE), "other aspect is preferred");
        cell.clearPartition();
        helper.assertTrue(cell.insert(key(Aspect.WATER), 5, Actionable.MODULATE, SOURCE) == 5, "cleared partition still restricts");
        cell.partitionToContents();
        helper.assertTrue(cell.getPartition().size() == 2, "partition to contents is wrong");
        helper.succeed();
    }

    private static void persistedToStack(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get());
        EssentiaCellInventory cell = EssentiaCellInventory.of(stack, null);
        cell.insert(key(Aspect.MAGIC), 77, Actionable.MODULATE, SOURCE);
        cell.addToPartition(Aspect.MAGIC);
        EssentiaCellInventory reloaded = EssentiaCellInventory.of(stack, null);
        helper.assertTrue(reloaded.getStoredEssentia().get(Aspect.MAGIC) == 77, "stored essentia not persisted");
        helper.assertTrue(reloaded.getPartition().equals(List.of(Aspect.MAGIC)), "partition not persisted");
        reloaded.extract(key(Aspect.MAGIC), 77, Actionable.MODULATE, SOURCE);
        reloaded.clearPartition();
        helper.assertTrue(!CellsFeature.CELL_DATA.has(stack), "empty cell keeps data");
        helper.succeed();
    }

    private static void rejectsItems(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.TYPE_1K);
        helper.assertTrue(cell.insert(AEItemKey.of(Items.DIAMOND), 1, Actionable.MODULATE, SOURCE) == 0, "cell accepted an item");
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        EssentiaCellInventory cell = cell(EssentiaStorageType.CREATIVE);
        cell.addToPartition(Aspect.FIRE);
        helper.assertTrue(cell.insert(key(Aspect.FIRE), 10, Actionable.MODULATE, SOURCE) == 0, "creative cell accepted essentia");
        helper.assertTrue(cell.extract(key(Aspect.FIRE), 123456, Actionable.MODULATE, SOURCE) == 123456, "creative cell is not infinite");
        helper.assertTrue(cell.extract(key(Aspect.WATER), 1, Actionable.MODULATE, SOURCE) == 0, "creative cell gave an unpartitioned aspect");
        KeyCounter counter = new KeyCounter();
        cell.getAvailableStacks(counter);
        helper.assertTrue(counter.get(key(Aspect.FIRE)) == EssentiaCellInventory.CREATIVE_AMOUNT, "creative amount is wrong");
        helper.assertTrue(cell.getStatus() == CellState.TYPES_FULL && cell.getTotalTypes() == 63, "creative cell status or types are wrong");
        helper.succeed();
    }

    private static void disassembleEmptyCell(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_16K).get()));
        ItemStack result = player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
        helper.assertTrue(result.is(CellsFeature.component(EssentiaStorageType.TYPE_16K).get()), "empty cell did not become a component: " + result);
        helper.assertTrue(player.getInventory().contains(new ItemStack(CellsFeature.HOUSING.get())), "housing was not returned");
        helper.succeed();
    }

    private static void fullCellNotDisassembled(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack stack = new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_1K).get());
        EssentiaCellInventory.of(stack, null).insert(key(Aspect.FIRE), 1, Actionable.MODULATE, SOURCE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ItemStack result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
        helper.assertTrue(result == stack, "non-empty cell was disassembled");
        helper.assertTrue(!player.getInventory().contains(new ItemStack(CellsFeature.HOUSING.get())), "housing was given for a non-empty cell");
        helper.succeed();
    }
}
