package thaumicenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.storage.MEStorage;
import appeng.api.util.AEColor;
import appeng.blockentity.storage.DriveBlockEntity;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.Aspect;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.provider.EssentiaProviderBlockEntity;
import thaumicenergistics.provider.InfusionProviderBlockEntity;
import thaumicenergistics.provider.ProviderBlock;
import thaumicenergistics.provider.ProviderBlockEntity;
import thaumicenergistics.provider.ProviderFeature;

public final class ProviderGameTests {
    private static final BlockPos ENERGY = new BlockPos(1, 1, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 1, 1);
    private static final BlockPos CABLE = new BlockPos(3, 1, 1);
    private static final BlockPos PROVIDER = new BlockPos(4, 1, 1);
    private static final IActionSource SOURCE = IActionSource.empty();

    private ProviderGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("provider")
            .add("research_and_recipes", ProviderGameTests::researchAndRecipes)
            .add("essentia_provider", ProviderGameTests::essentiaProvider)
            .add("infusion_provider", ProviderGameTests::infusionProvider)
            .register(bus);
    }

    private static void researchAndRecipes(GameTestHelper helper) {
        ResearchItem essentia = ResearchCategories.getResearch("thaumicenergistics.TEESSPROV");
        helper.assertTrue(essentia != null && essentia.column() == -2 && essentia.row() == -4 && essentia.complexity() == 3 && essentia.isConcealed(), "essentia provider research is wrong");
        helper.assertTrue(essentia.parents().equals(List.of("thaumicenergistics.TEIO")) && essentia.parentsHidden().equals(List.of("INFUSION", "TUBEFILTER")), "essentia provider parents are wrong");
        ResearchItem infusion = ResearchCategories.getResearch("thaumicenergistics.TEINFPROV");
        helper.assertTrue(infusion != null && infusion.column() == -5 && infusion.row() == -2 && infusion.isSpecial(), "infusion provider research is wrong");
        helper.assertTrue(infusion.parents().equals(List.of("thaumicenergistics.TEIO", "thaumicenergistics.Pseudo.MIRROR", "thaumicenergistics.Pseudo.INFUSION")), "infusion provider parents are " + infusion.parents());
        helper.assertTrue(TcRecipes.get(ProviderFeature.ESSENTIA_PROVIDER_RECIPE) != null && TcRecipes.get(ProviderFeature.INFUSION_PROVIDER_RECIPE) != null, "provider recipes are missing");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <T extends ProviderBlockEntity> void network(GameTestHelper helper, Block block, Consumer<T> body) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(ENERGY, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:creative_energy_cell")));
        helper.setBlock(DRIVE, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("ae2:drive")));
        DriveBlockEntity drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE);
        drive.getInternalInventory().insertItem(0, new ItemStack(CellsFeature.cell(EssentiaStorageType.TYPE_4K).get()), false);
        helper.setBlock(PROVIDER, block);
        helper.runAfterDelay(5, () -> PartHelper.setPart(helper.getLevel(), helper.absolutePos(CABLE), null, player, (IPartItem<?>) BuiltInRegistries.ITEM.get(ResourceLocation.parse("ae2:fluix_glass_cable"))));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(GridHelper.getNodeHost(helper.getLevel(), helper.absolutePos(PROVIDER)) != null, "provider exposes no grid node host capability");
            T provider = (T) helper.getBlockEntity(PROVIDER);
            helper.assertTrue(provider.isActive(), "provider is not active in the network");
            helper.assertTrue(helper.getBlockState(PROVIDER).getValue(ProviderBlock.ACTIVE), "provider block state is not active");
            body.accept(provider);
            helper.succeed();
        });
    }

    private static MEStorage storage(ProviderBlockEntity provider) {
        IGrid grid = provider.getMainNode().getGrid();
        return grid.getStorageService().getInventory();
    }

    private static void essentiaProvider(GameTestHelper helper) {
        network(helper, ProviderFeature.ESSENTIA_PROVIDER.get(), (EssentiaProviderBlockEntity provider) -> {
            MEStorage storage = storage(provider);
            storage.insert(EssentiaKey.of(Aspect.FIRE), 20, Actionable.MODULATE, SOURCE);
            helper.assertTrue(provider.takeEssentia(Aspect.FIRE, 5, Direction.UP) == 5, "provider did not give fire");
            helper.assertTrue(provider.takeEssentia(Aspect.FIRE, 50, Direction.UP) == 15, "provider did not give the rest of the fire");
            helper.assertTrue(provider.addEssentia(Aspect.WATER, 3, Direction.UP) == 3, "provider did not accept water");
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.WATER), 10, Actionable.SIMULATE, SOURCE) == 3, "water did not reach the network");
            helper.assertTrue(provider.getSuctionAmount(Direction.UP) == 8 && provider.getSuctionType(Direction.UP) == null, "provider suction is wrong");
            provider.recolourBlock(Direction.UP, AEColor.RED, null);
            helper.assertTrue(provider.getColor() == AEColor.RED, "provider was not recoloured");
        });
    }

    private static void infusionProvider(GameTestHelper helper) {
        network(helper, ProviderFeature.INFUSION_PROVIDER.get(), (InfusionProviderBlockEntity provider) -> {
            MEStorage storage = storage(provider);
            storage.insert(EssentiaKey.of(Aspect.ORDER), 20, Actionable.MODULATE, SOURCE);
            helper.assertTrue(provider.takeFromContainer(Aspect.ORDER, 5), "infusion provider did not give order");
            helper.assertTrue(!provider.takeFromContainer(Aspect.ORDER, 50), "infusion provider gave a partial amount");
            helper.assertTrue(storage.extract(EssentiaKey.of(Aspect.ORDER), 100, Actionable.SIMULATE, SOURCE) == 15, "network order is wrong");
            helper.assertTrue(provider.addToContainer(Aspect.ORDER, 4) == 4, "infusion provider swallowed essentia");
        });
    }
}
