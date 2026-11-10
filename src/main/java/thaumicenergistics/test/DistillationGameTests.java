package thaumicenergistics.test;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ScanKind;
import thaumcraft.research.ScanManager;
import thaumicenergistics.distillation.DistillationEncoderBlockEntity;
import thaumicenergistics.distillation.DistillationEncoderMenu;
import thaumicenergistics.distillation.DistillationFeature;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.research.TeResearch;

public final class DistillationGameTests {
    private static final BlockPos ENCODER = new BlockPos(1, 1, 1);
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos CABLE = new BlockPos(2, 1, 1);
    private static final BlockPos PROVIDER = new BlockPos(3, 1, 1);

    private DistillationGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("distillation")
            .add("research_and_recipe", DistillationGameTests::researchAndRecipe)
            .add("encode_pattern", DistillationGameTests::encodePattern)
            .add("network_craftable", DistillationGameTests::networkCraftable)
            .register(bus);
    }

    private static void researchAndRecipe(GameTestHelper helper) {
        ResearchItem research = ResearchCategories.getResearch(TeResearch.DISTILLATION_ENCODER);
        helper.assertTrue(research != null && research.column() == -4 && research.row() == -4 && research.isConcealed(), "encoder research is wrong");
        helper.assertTrue(research.parents().equals(List.of(TeResearch.IO)) && research.parentsHidden().equals(List.of(TeResearch.ESSENTIA_TERMINAL)), "encoder parents are wrong");
        helper.assertTrue(TcRecipes.get(DistillationFeature.RECIPE) != null, "encoder recipe is missing");
        helper.succeed();
    }

    private static ItemStack encode(Aspect aspect, int amount) {
        return PatternDetailsHelper.encodeProcessingPattern(
            List.of(new GenericStack(AEItemKey.of(Items.STONE), 1)),
            List.of(new GenericStack(EssentiaKey.of(aspect), amount))
        );
    }

    private static void encodePattern(GameTestHelper helper) {
        helper.setBlock(ENCODER, DistillationFeature.ENCODER.get());
        DistillationEncoderBlockEntity encoder = (DistillationEncoderBlockEntity) helper.getBlockEntity(ENCODER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        DistillationEncoderMenu menu = new DistillationEncoderMenu(1, player.getInventory(), helper.absolutePos(ENCODER));
        encoder.setItem(DistillationEncoderBlockEntity.SLOT_BLANK_PATTERNS, AEItems.BLANK_PATTERN.stack(2));
        menu.setCarried(new ItemStack(Items.STONE, 5));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.broadcastChanges();
        helper.assertTrue(menu.sourceAspects() != null && menu.sourceAspects().get(0).aspect() == null && menu.selectedAspect() == null, "unscanned item reveals aspects");
        menu.clickMenuButton(player, DistillationEncoderMenu.BUTTON_ENCODE);
        helper.assertTrue(encoder.getItem(DistillationEncoderBlockEntity.SLOT_ENCODED_PATTERN).isEmpty(), "unknown aspect was encoded");
        Knowledge.get(player).addScanned(ScanKind.OBJECT, ScanManager.itemKey(new ItemStack(Items.STONE)));
        Knowledge.get(player).discoverAspect(Aspect.EARTH);
        menu.setCarried(new ItemStack(Items.DIRT));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.broadcastChanges();
        menu.setCarried(new ItemStack(Items.STONE));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        menu.setCarried(ItemStack.EMPTY);
        menu.broadcastChanges();
        helper.assertTrue(menu.selectedAspect() != null && menu.selectedAspect().aspect() == Aspect.EARTH, "stone aspect was not selected");
        menu.clickMenuButton(player, DistillationEncoderMenu.BUTTON_ENCODE);
        ItemStack pattern = encoder.getItem(DistillationEncoderBlockEntity.SLOT_ENCODED_PATTERN);
        helper.assertTrue(encoder.getItem(DistillationEncoderBlockEntity.SLOT_BLANK_PATTERNS).getCount() == 1, "blank pattern was not used");
        helper.assertTrue(PatternDetailsHelper.decodePattern(pattern, helper.getLevel()) instanceof AEProcessingPattern decoded
            && decoded.getOutputs().equals(List.of(new GenericStack(EssentiaKey.of(Aspect.EARTH), 2)))
            && decoded.getSparseInputs().get(0).equals(new GenericStack(AEItemKey.of(Items.STONE), 1)), "pattern contents are wrong");
        encoder.setItem(DistillationEncoderBlockEntity.SLOT_SOURCE_ITEM, ItemStack.EMPTY);
        menu.broadcastChanges();
        encoder.setItem(DistillationEncoderBlockEntity.SLOT_ENCODED_PATTERN, pattern.copy());
        menu.broadcastChanges();
        helper.assertTrue(menu.sourceItem().is(Items.STONE) && menu.selectedAspect() != null && menu.selectedAspect().aspect() == Aspect.EARTH, "pattern did not load back");
        helper.succeed();
    }

    private static void networkCraftable(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(PROVIDER, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:pattern_provider")));
        PatternProviderBlockEntity provider = (PatternProviderBlockEntity) helper.getBlockEntity(PROVIDER);
        provider.getLogic().getPatternInv().insertItem(0, encode(Aspect.EARTH, 2), false);
        helper.runAfterDelay(5, () -> PartHelper.setPart(helper.getLevel(), helper.absolutePos(CABLE), null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable"))));
        helper.runAfterDelay(25, () -> {
            IGrid grid = provider.getMainNode().getGrid();
            helper.assertTrue(grid.getCraftingService().isCraftable(EssentiaKey.of(Aspect.EARTH)), "essentia is not craftable in the network");
            helper.succeed();
        });
    }
}
