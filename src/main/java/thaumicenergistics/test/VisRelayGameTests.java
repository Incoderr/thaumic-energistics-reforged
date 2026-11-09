package thaumicenergistics.test;

import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.vis.VisNet;
import thaumcraft.vis.VisNode;
import thaumcraft.vis.VisRelayBlock;
import thaumcraft.vis.VisRelayBlockEntity;
import thaumcraft.wand.Wands;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.act.ArcaneCraftingTerminalPart;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.visrelay.VisRelayFeature;
import thaumicenergistics.visrelay.VisRelayInterfacePart;
import thaumicenergistics.visrelay.VisSourceLink;

public final class VisRelayGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos ENERGY_HIGH = new BlockPos(3, 2, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos RELAY_EAST = CABLE.east();
    private static final BlockPos ROOT_POS = RELAY_EAST.east();
    private static final BlockPos RELAY_WEST = CABLE.west();
    private static final BlockPos ROOT_WEST = RELAY_WEST.west();

    private VisRelayGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("visrelay")
            .add("research_and_recipe", VisRelayGameTests::researchAndRecipe)
            .add("drains_from_relay", VisRelayGameTests::drainsFromRelay)
            .add("p2p_provider", VisRelayGameTests::p2pProvider)
            .add("terminal_refill", VisRelayGameTests::terminalRefill)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        ResearchItem item = ResearchCategories.getResearch(TeResearch.VIS_RELAY_INTERFACE);
        helper.assertTrue(item != null && item.column() == 4 && item.row() == -1 && item.complexity() == 2, "vis relay interface research is wrong");
        helper.assertTrue(
            item.parents().equals(List.of(TeResearch.ARCANE_TERMINAL, TeResearch.pseudo(TeResearch.VISPOWER))),
            "vis relay interface parents are " + item.parents()
        );
        helper.assertTrue(TcRecipes.get(VisRelayFeature.RECIPE) != null, "vis relay interface recipe is missing");
        helper.succeed();
    }

    /**
     * A vis source that never runs dry.
     */
    private static final class RootSource extends VisNode {
        private RootSource(BlockPos pos, Level level) {
            super(BlockEntityType.BEACON, pos, Blocks.BEACON.defaultBlockState());
            setLevel(level);
        }

        @Override
        public int getRange() {
            return 8;
        }

        @Override
        public boolean isSource() {
            return true;
        }

        @Override
        public int consumeVis(Aspect aspect, int vis) {
            return vis;
        }
    }

    private static void placeRelay(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:vis_relay")).defaultBlockState()
            .setValue(VisRelayBlock.FACING, facing);
        helper.setBlock(pos, state);
    }

    private static void placeCable(GameTestHelper helper, Player player, BlockPos absoluteCable) {
        PartHelper.setPart(helper.getLevel(), absoluteCable, null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable")));
    }

    private static ItemStack linkedCard(VisRelayInterfacePart source) {
        ItemStack card = item("ae2:memory_card");
        VisRelayFeature.LINK.set(card, new VisSourceLink(source).writeToTag());
        return card;
    }

    private static void connected(GameTestHelper helper, BlockPos helperPos, String message) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(helperPos));
        helper.assertTrue(state.getValue(VisRelayBlock.CONNECTED), message);
    }

    private static void drainsFromRelay(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY_HIGH, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        BlockPos cable = helper.absolutePos(CABLE);
        placeCable(helper, player, cable);
        placeRelay(helper, RELAY_EAST, Direction.EAST);
        VisNet.addSource(helper.getLevel(), new RootSource(helper.absolutePos(ROOT_POS), helper.getLevel()));
        VisRelayInterfacePart part = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, VisRelayFeature.INTERFACE.get());
        helper.assertTrue(part != null, "interface was not placed");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(part.isActive(), "interface is not active");
            connected(helper, RELAY_EAST, "relay did not connect to the vis source");
            int drained = part.consumeVis(Aspect.FIRE, 8);
            helper.assertTrue(drained == 8, "relay drained " + drained + " vis");
            helper.succeed();
        });
    }

    private static void p2pProvider(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY_HIGH, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        BlockPos cable = helper.absolutePos(CABLE);
        placeCable(helper, player, cable);
        placeRelay(helper, RELAY_EAST, Direction.EAST);
        placeRelay(helper, RELAY_WEST, Direction.WEST);
        VisNet.addSource(helper.getLevel(), new RootSource(helper.absolutePos(ROOT_POS), helper.getLevel()));
        VisRelayInterfacePart source = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, VisRelayFeature.INTERFACE.get());
        VisRelayInterfacePart provider = PartHelper.setPart(helper.getLevel(), cable, Direction.WEST, player, VisRelayFeature.INTERFACE.get());
        helper.assertTrue(source != null && provider != null, "interfaces were not placed");
        helper.runAfterDelay(20, () -> {
            provider.onUseItemOn(linkedCard(source), player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            helper.assertTrue(provider.isVisProvider(), "interface did not load the vis source link");
            helper.assertTrue(!source.isVisProvider(), "source interface should not be a provider");
        });
        helper.runAfterDelay(80, () -> {
            connected(helper, RELAY_WEST, "provider relay did not connect to the digital source");
            VisRelayBlockEntity relay = (VisRelayBlockEntity) helper.getBlockEntity(RELAY_WEST);
            int drained = relay.consumeVis(Aspect.FIRE, 8);
            helper.assertTrue(drained == 8, "digital vis chain drained " + drained + " vis");
            helper.succeed();
        });
    }

    private static void terminalRefill(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY_HIGH, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        BlockPos cable = helper.absolutePos(CABLE);
        placeCable(helper, player, cable);
        placeRelay(helper, RELAY_WEST, Direction.WEST);
        VisNet.addSource(helper.getLevel(), new RootSource(helper.absolutePos(ROOT_WEST), helper.getLevel()));
        ArcaneCraftingTerminalPart terminal = PartHelper.setPart(helper.getLevel(), cable, Direction.EAST, player, ActFeature.TERMINAL.get());
        VisRelayInterfacePart source = PartHelper.setPart(helper.getLevel(), cable, Direction.WEST, player, VisRelayFeature.INTERFACE.get());
        helper.assertTrue(terminal != null && source != null, "terminal or interface was not placed");
        terminal.onUseItemOn(linkedCard(source), player, InteractionHand.MAIN_HAND, Vec3.ZERO);
        ItemStack wand = item("thaumcraft:wand");
        terminal.wandInventory().setItemDirect(0, wand);
        helper.runAfterDelay(40, () -> {
            int expected = Wands.getMaxVis(wand);
            helper.assertTrue(Wands.getVis(wand, Aspect.FIRE) == expected, "wand fire vis is " + Wands.getVis(wand, Aspect.FIRE) + "/" + expected);
            helper.assertTrue(Wands.getVis(wand, Aspect.EARTH) == expected, "wand earth vis is " + Wands.getVis(wand, Aspect.EARTH) + "/" + expected);
            helper.succeed();
        });
    }
}
