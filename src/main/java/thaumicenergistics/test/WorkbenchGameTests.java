package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.item.AspectTooltips;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaCellInventory;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaItems;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.workbench.EssentiaCellWorkbenchBlockEntity;
import thaumicenergistics.workbench.EssentiaCellWorkbenchMenu;
import thaumicenergistics.workbench.WorkbenchActionPayload;
import thaumicenergistics.workbench.WorkbenchFeature;

public final class WorkbenchGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private WorkbenchGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("workbench")
            .add("filter_aspect_from_items", WorkbenchGameTests::filterAspectFromItems)
            .add("slot_accepts_only_cells", WorkbenchGameTests::slotAcceptsOnlyCells)
            .add("click_slot_actions", WorkbenchGameTests::clickSlotActions)
            .add("clear_and_partition_to_contents", WorkbenchGameTests::clearAndPartitionToContents)
            .add("click_without_item_or_aspect_does_nothing", WorkbenchGameTests::clickWithoutItemOrAspect)
            .add("shift_click_adds_aspect", WorkbenchGameTests::shiftClickAddsAspect)
            .add("cell_dropped_on_break", WorkbenchGameTests::cellDroppedOnBreak)
            .add("wrench_dismantles", WorkbenchGameTests::wrenchDismantles)
            .add("recipe_loaded", WorkbenchGameTests::recipeLoaded)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ItemStack label(Aspect aspect) {
        ItemStack stack = item("thaumcraft:jar_label");
        AspectTooltips.setSingle(stack, aspect, 1);
        return stack;
    }

    private static ItemStack cell() {
        return new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_1K).get());
    }

    private static EssentiaCellWorkbenchBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, WorkbenchFeature.BLOCK.get());
        return (EssentiaCellWorkbenchBlockEntity) helper.getBlockEntity(POS);
    }

    private static EssentiaCellWorkbenchMenu menu(GameTestHelper helper, Player player) {
        return new EssentiaCellWorkbenchMenu(1, player.getInventory(), helper.absolutePos(POS));
    }

    private static List<Aspect> partition(EssentiaCellWorkbenchBlockEntity workbench) {
        return EssentiaCellInventory.of(workbench.getItem(0), null).getPartition();
    }

    private static void filterAspectFromItems(GameTestHelper helper) {
        helper.assertTrue(EssentiaItems.filterAspect(label(Aspect.FIRE)) == Aspect.FIRE, "label aspect is not read");
        ItemStack essence = item("thaumcraft:wisp_essence");
        AspectTooltips.setSingle(essence, Aspect.MAGIC, 1);
        helper.assertTrue(EssentiaItems.filterAspect(essence) == Aspect.MAGIC, "wisp essence aspect is not read");
        helper.assertTrue(EssentiaItems.filterAspect(item("thaumcraft:jar_label")) == null, "blank label has an aspect");
        ItemStack dirt = new ItemStack(Blocks.DIRT);
        AspectTooltips.setSingle(dirt, Aspect.EARTH, 1);
        helper.assertTrue(EssentiaItems.filterAspect(dirt) == null, "unrelated item gives an aspect");
        helper.succeed();
    }

    private static void slotAcceptsOnlyCells(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        helper.assertTrue(workbench.canPlaceItem(0, cell()), "cell is rejected");
        helper.assertTrue(workbench.canPlaceItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.CREATIVE).get())), "creative cell is rejected");
        helper.assertTrue(!workbench.canPlaceItem(0, new ItemStack(CellsFeature.HOUSING.get())), "housing is accepted");
        helper.assertTrue(workbench.getMaxStackSize() == 1, "slot holds more than one item");
        helper.succeed();
    }

    private static void clickSlotActions(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        workbench.setItem(0, cell());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EssentiaCellWorkbenchMenu menu = menu(helper, player);
        menu.setCarried(label(Aspect.FIRE));
        menu.handleAction(WorkbenchActionPayload.CLICK_SLOT, null);
        helper.assertTrue(partition(workbench).equals(List.of(Aspect.FIRE)), "add gave " + partition(workbench));
        menu.setCarried(label(Aspect.WATER));
        menu.handleAction(WorkbenchActionPayload.CLICK_SLOT, Aspect.FIRE);
        helper.assertTrue(partition(workbench).equals(List.of(Aspect.WATER)), "replace gave " + partition(workbench));
        menu.setCarried(ItemStack.EMPTY);
        menu.handleAction(WorkbenchActionPayload.CLICK_SLOT, Aspect.WATER);
        helper.assertTrue(partition(workbench).isEmpty(), "remove gave " + partition(workbench));
        helper.succeed();
    }

    private static void clearAndPartitionToContents(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        ItemStack stack = cell();
        EssentiaCellInventory inventory = EssentiaCellInventory.of(stack, null);
        inventory.insert(EssentiaKey.of(Aspect.MAGIC), 10, Actionable.MODULATE, IActionSource.empty());
        inventory.insert(EssentiaKey.of(Aspect.AIR), 10, Actionable.MODULATE, IActionSource.empty());
        workbench.setItem(0, stack);
        EssentiaCellWorkbenchMenu menu = menu(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        menu.handleAction(WorkbenchActionPayload.PARTITION_TO_CONTENTS, null);
        helper.assertTrue(partition(workbench).equals(List.of(Aspect.MAGIC, Aspect.AIR)), "partition to contents gave " + partition(workbench));
        menu.handleAction(WorkbenchActionPayload.CLEAR, null);
        helper.assertTrue(partition(workbench).isEmpty(), "clear left " + partition(workbench));
        helper.succeed();
    }

    private static void clickWithoutItemOrAspect(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        workbench.setItem(0, cell());
        EssentiaCellWorkbenchMenu menu = menu(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        menu.setCarried(new ItemStack(Blocks.DIRT));
        menu.handleAction(WorkbenchActionPayload.CLICK_SLOT, null);
        menu.handleAction(WorkbenchActionPayload.CLICK_SLOT, Aspect.FIRE);
        helper.assertTrue(partition(workbench).isEmpty(), "partition changed to " + partition(workbench));
        helper.succeed();
    }

    private static void shiftClickAddsAspect(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, cell());
        player.getInventory().setItem(1, label(Aspect.ORDER));
        EssentiaCellWorkbenchMenu menu = menu(helper, player);
        menu.quickMoveStack(player, 28);
        helper.assertTrue(workbench.hasCell() && player.getInventory().getItem(0).isEmpty(), "cell was not moved into the workbench");
        menu.quickMoveStack(player, 29);
        helper.assertTrue(partition(workbench).equals(List.of(Aspect.ORDER)), "shift click gave " + partition(workbench));
        helper.assertTrue(!player.getInventory().getItem(1).isEmpty(), "label was consumed");
        helper.succeed();
    }

    private static void cellDroppedOnBreak(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        workbench.setItem(0, cell());
        helper.setBlock(POS, Blocks.AIR);
        helper.assertItemEntityPresent(CellsFeature.cell(EssentiaStorageType.TYPE_1K).get(), POS, 2.0);
        helper.succeed();
    }

    private static void wrenchDismantles(GameTestHelper helper) {
        EssentiaCellWorkbenchBlockEntity workbench = place(helper);
        workbench.setItem(0, cell());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        ItemStack wrench = item("ae2:certus_quartz_wrench");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos absolute = helper.absolutePos(POS);
        helper.getLevel().getBlockState(absolute).useItemOn(wrench, helper.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertBlockNotPresent(WorkbenchFeature.BLOCK.get(), POS);
        helper.assertItemEntityPresent(WorkbenchFeature.BLOCK.get().asItem(), POS, 2.0);
        helper.assertItemEntityPresent(CellsFeature.cell(EssentiaStorageType.TYPE_1K).get(), POS, 2.0);
        helper.succeed();
    }

    private static void recipeLoaded(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ThaumicEnergistics.id("essentia_cell_workbench")).isPresent(), "workbench recipe is not loaded");
        helper.succeed();
    }
}
