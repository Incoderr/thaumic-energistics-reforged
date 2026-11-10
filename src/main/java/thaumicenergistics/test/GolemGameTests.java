package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.entity.golem.GolemBase;
import thaumcraft.entity.golem.GolemSnapshot;
import thaumcraft.entity.golem.GolemType;
import thaumcraft.registry.ModEntities;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.golem.BackpackData;
import thaumicenergistics.golem.BackpackSkin;
import thaumicenergistics.golem.GolemBackpackFeature;
import thaumicenergistics.research.TeResearch;

public final class GolemGameTests {
    private static final BlockPos ACCESS_POINT = new BlockPos(2, 1, 2);

    private GolemGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("golem")
            .add("research_and_recipe", GolemGameTests::researchAndRecipe)
            .add("gather_golem_deposits_to_network", GolemGameTests::gatherDeposits)
            .add("facade_skins", GolemGameTests::facadeSkins)
            .register(bus);
    }

    private static Block ae(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ae2", path));
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch(TeResearch.GOLEM_BACKPACK);
        helper.assertTrue(item != null && item.column() == 0 && item.row() == -6 && item.complexity() == 3, "backpack research is wrong");
        helper.assertTrue(item.parents().equals(List.of(TeResearch.CORES, "COREGATHER", "thaumicenergistics.Pseudo.COREGATHER")), "backpack parents are " + item.parents());
        helper.assertTrue(TcRecipes.get(GolemBackpackFeature.BACKPACK_RECIPE) != null, "backpack recipe is missing");
        helper.succeed();
    }

    private static void facadeSkins(GameTestHelper helper) {
        helper.assertTrue(BackpackSkin.fromBlock(net.minecraft.world.level.block.Blocks.GOLD_BLOCK) == BackpackSkin.GOLD, "gold skin is wrong");
        Block flesh = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "flesh_block"));
        helper.assertTrue(BackpackSkin.fromBlock(flesh) == BackpackSkin.FLESH, "flesh skin is wrong");
        helper.assertTrue(BackpackSkin.fromBlock(net.minecraft.world.level.block.Blocks.DIRT) == null, "dirt gave a skin");
        helper.assertTrue(GolemBackpackFeature.skinOf(ItemStack.EMPTY) == BackpackSkin.THAUMIUM, "default skin is wrong");
        helper.succeed();
    }

    private static void gatherDeposits(GameTestHelper helper) {
        helper.setBlock(ACCESS_POINT, ae("wireless_access_point"));
        Direction back = BlockOrientation.get(helper.getBlockState(ACCESS_POINT)).getSide(RelativeSide.BACK);
        BlockPos energy = ACCESS_POINT.relative(back);
        helper.setBlock(energy, ae("creative_energy_cell"));
        BlockPos drive = energy.above();
        helper.setBlock(drive, ae("drive"));
        ((DriveBlockEntity) helper.getBlockEntity(drive)).getInternalInventory().insertItem(0, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:item_storage_cell_1k"))), false);
        GolemBase golem = ModEntities.GOLEM.get().create(helper.getLevel());
        BlockPos golemPos = helper.absolutePos(new BlockPos(2, 1, 4));
        golem.moveTo(golemPos.getX() + 0.5, golemPos.getY(), golemPos.getZ() + 0.5);
        golem.configure(GolemType.STRAW, new GolemSnapshot(2, false, List.of(), "", List.of(), net.minecraft.world.item.component.ItemContainerContents.EMPTY, List.of(), 0));
        golem.setHome(golemPos, Direction.UP);
        helper.getLevel().addFreshEntity(golem);
        helper.runAfterDelay(20, () -> {
            WirelessAccessPointBlockEntity wap = (WirelessAccessPointBlockEntity) helper.getBlockEntity(ACCESS_POINT);
            helper.assertTrue(wap.isActive(), "access point is not active");
            ItemStack backpack = GolemBackpackFeature.BACKPACK.toStack();
            backpack.set(AEComponents.WIRELESS_LINK_TARGET, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(ACCESS_POINT)));
            helper.assertTrue(GolemBackpackFeature.attach(golem, backpack), "backpack was not attached");
            golem.setCarried(new ItemStack(Items.COBBLESTONE, 5));
            helper.runAfterDelay(60, () -> {
                IGrid grid = wap.getGrid();
                long stored = grid.getStorageService().getInventory().extract(AEItemKey.of(Items.COBBLESTONE), 64, Actionable.SIMULATE, IActionSource.empty());
                helper.assertTrue(stored == 5, "network holds " + stored + " cobblestone, golem carries " + golem.getCarried());
                helper.assertTrue(golem.getCarried().isEmpty(), "golem still carries " + golem.getCarried());
                BackpackData.Client state = GolemBackpackFeature.CLIENT.get(golem);
                helper.assertTrue(state != null && state.inRange(), "client state is " + state);
                helper.succeed();
            });
        });
    }
}
