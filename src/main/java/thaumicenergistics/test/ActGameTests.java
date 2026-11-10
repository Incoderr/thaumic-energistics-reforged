package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.blockentity.storage.DriveBlockEntity;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.Knowledge;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.act.ArcaneCraftingTermMenu;
import thaumicenergistics.act.ArcaneCraftingTerminalPart;
import thaumicenergistics.research.TeResearch;

public final class ActGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private ActGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("act")
            .add("research_and_recipe", ActGameTests::researchAndRecipe)
            .add("vanilla_crafting_refills", ActGameTests::vanillaCraftingRefills)
            .add("arcane_cost_without_wand", ActGameTests::arcaneCostWithoutWand)
            .add("clear_grid_and_swap_armor", ActGameTests::clearGridAndSwapArmor)
            .add("fill_grid_from_recipe", ActGameTests::fillGridFromRecipe)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch("thaumicenergistics.TEARCANETERM");
        helper.assertTrue(item != null && item.column() == 2 && item.row() == -1 && item.complexity() == 1, "arcane terminal research is wrong");
        helper.assertTrue(item.parents().equals(List.of(TeResearch.BASIC)), "arcane terminal parents are " + item.parents());
        helper.assertTrue(TcRecipes.get(ActFeature.RECIPE) != null, "arcane terminal recipe is missing");
        helper.succeed();
    }

    private static void network(GameTestHelper helper, BiConsumer<ArcaneCraftingTerminalPart, IGrid> body) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, item("ae2:item_storage_cell_1k"), false);
        BlockPos cable = helper.absolutePos(CABLE);
        PartHelper.setPart(helper.getLevel(), cable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
        ArcaneCraftingTerminalPart part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, ActFeature.TERMINAL.get());
        helper.assertTrue(part != null, "terminal was not placed");
        helper.runAfterDelay(20, () -> {
            IGrid grid = part.getMainNode().getGrid();
            helper.assertTrue(grid != null && part.isActive(), "terminal is not active");
            body.accept(part, grid);
            helper.succeed();
        });
    }

    private static ArcaneCraftingTermMenu menu(GameTestHelper helper, ArcaneCraftingTerminalPart part) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ArcaneCraftingTermMenu menu = new ArcaneCraftingTermMenu(1, player.getInventory(), part);
        menu.broadcastChanges();
        return menu;
    }

    private static int resultIndex(ArcaneCraftingTermMenu menu) {
        return menu.slots.indexOf(menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_RESULT).get(0));
    }

    private static void vanillaCraftingRefills(GameTestHelper helper) {
        network(helper, (part, grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            storage.insert(AEItemKey.of(Items.OAK_PLANKS), 8, Actionable.MODULATE, SOURCE);
            for (int slot : new int[] {0, 1, 3, 4}) {
                part.craftingGrid().setItemDirect(slot, new ItemStack(Items.OAK_PLANKS));
            }
            ArcaneCraftingTermMenu menu = menu(helper, part);
            menu.onSlotChange(menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_GRID).get(0));
            helper.assertTrue(menu.slots.get(resultIndex(menu)).getItem().is(Items.CRAFTING_TABLE), "crafting table is not offered");
            menu.quickMoveStack(menu.getPlayer(), resultIndex(menu));
            helper.assertTrue(menu.getPlayer().getInventory().countItem(Items.CRAFTING_TABLE) == 3, "shift craft made " + menu.getPlayer().getInventory().countItem(Items.CRAFTING_TABLE));
            helper.assertTrue(storage.extract(AEItemKey.of(Items.OAK_PLANKS), 100, Actionable.SIMULATE, SOURCE) == 0, "network planks were not used for refills");
            helper.assertTrue(part.craftingGrid().getStackInSlot(0).isEmpty(), "grid was not emptied");
        });
    }

    private static void arcaneCostWithoutWand(GameTestHelper helper) {
        network(helper, (part, grid) -> {
            ArcaneCraftingTermMenu menu = menu(helper, part);
            Knowledge.get(menu.getPlayer()).addResearch(TeResearch.ARCANE_TERMINAL);
            part.craftingGrid().setItemDirect(0, item("ae2:terminal"));
            part.craftingGrid().setItemDirect(1, item("thaumcraft:arcane_worktable"));
            part.craftingGrid().setItemDirect(2, item("ae2:calculation_processor"));
            menu.onSlotChange(menu.getSlots(appeng.menu.SlotSemantics.CRAFTING_GRID).get(0));
            helper.assertTrue(menu.slots.get(resultIndex(menu)).getItem().isEmpty(), "arcane result offered without a wand");
            List<ArcaneCraftingTermMenu.Cost> costs = menu.clientCosts();
            helper.assertTrue(costs.size() == 6 && costs.stream().noneMatch(ArcaneCraftingTermMenu.Cost::enough), "costs are " + menu.costs);
            helper.assertTrue(costs.stream().anyMatch(cost -> cost.aspect() == Aspect.FIRE && cost.vis() == 10.0F), "fire cost is wrong: " + menu.costs);
        });
    }

    private static void fillGridFromRecipe(GameTestHelper helper) {
        network(helper, (part, grid) -> {
            MEStorage storage = grid.getStorageService().getInventory();
            storage.insert(AEItemKey.of(Items.OAK_PLANKS), 2, Actionable.MODULATE, SOURCE);
            ArcaneCraftingTermMenu menu = menu(helper, part);
            part.craftingGrid().setItemDirect(8, new ItemStack(Items.COBBLESTONE));
            menu.getPlayer().getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 2));
            menu.getPlayer().getInventory().add(new ItemStack(Items.OAK_PLANKS, 2));
            List<List<ItemStack>> slots = new java.util.ArrayList<>();
            for (int index = 0; index < 9; index++) {
                slots.add(List.of());
            }
            List<ItemStack> planks = List.of(new ItemStack(Items.SPRUCE_PLANKS), new ItemStack(Items.OAK_PLANKS));
            for (int index : new int[] {0, 1, 3, 4}) {
                slots.set(index, planks);
            }
            menu.fillGrid(menu.getPlayer(), slots);
            for (int index : new int[] {0, 1, 3, 4}) {
                helper.assertTrue(part.craftingGrid().getStackInSlot(index).is(Items.OAK_PLANKS), "slot " + index + " holds " + part.craftingGrid().getStackInSlot(index));
            }
            helper.assertTrue(part.craftingGrid().getStackInSlot(8).isEmpty(), "old grid item stayed");
            helper.assertTrue(storage.extract(AEItemKey.of(Items.COBBLESTONE), 10, Actionable.SIMULATE, SOURCE) == 1, "old grid item was not returned");
            helper.assertTrue(storage.extract(AEItemKey.of(Items.OAK_PLANKS), 10, Actionable.SIMULATE, SOURCE) == 0, "network planks were not used");
            helper.assertTrue(menu.getPlayer().getInventory().countItem(Items.BIRCH_PLANKS) == 2, "unlisted planks were used");
        });
    }

    private static void clearGridAndSwapArmor(GameTestHelper helper) {
        network(helper, (part, grid) -> {
            ArcaneCraftingTermMenu menu = menu(helper, part);
            part.craftingGrid().setItemDirect(4, new ItemStack(Items.COBBLESTONE, 5));
            menu.clearCraftingGrid();
            helper.assertTrue(part.craftingGrid().getStackInSlot(4).isEmpty(), "grid was not cleared");
            helper.assertTrue(grid.getStorageService().getInventory().extract(AEItemKey.of(Items.COBBLESTONE), 10, Actionable.SIMULATE, SOURCE) == 5, "cleared items did not reach the network");
            Player player = menu.getPlayer();
            part.armorInventory().setItemDirect(0, new ItemStack(Items.IRON_HELMET));
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            menu.swapArmor();
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && part.armorInventory().getStackInSlot(0).is(Items.GOLDEN_HELMET), "armor was not swapped");
            helper.assertTrue(!ArcaneCraftingTerminalPart.isArmorFor(new ItemStack(Items.IRON_BOOTS), 0), "boots fit the helmet slot");
        });
    }
}
