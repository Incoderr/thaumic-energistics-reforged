package thaumicenergistics.test;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.research.Knowledge;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.knowledge.ArcanePattern;
import thaumicenergistics.knowledge.KnowledgeCore;
import thaumicenergistics.knowledge.KnowledgeFeature;
import thaumicenergistics.knowledge.KnowledgeInscriberBlockEntity;
import thaumicenergistics.knowledge.KnowledgeInscriberMenu;
import thaumicenergistics.research.TeResearch;

public final class KnowledgeGameTests {
    private static final BlockPos INSCRIBER = new BlockPos(1, 1, 1);

    private KnowledgeGameTests() {
    }

    public static void register(IEventBus bus) {
        new TeGameTestSuite("knowledge")
            .add("research_and_recipes", KnowledgeGameTests::researchAndRecipes)
            .add("core_storage", KnowledgeGameTests::coreStorage)
            .add("inscriber_save_and_delete", KnowledgeGameTests::inscriberSaveAndDelete)
            .register(bus);
    }

    private static ItemStack item(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static void researchAndRecipes(GameTestHelper helper) {
        ResearchItem inscriber = ResearchCategories.getResearch(TeResearch.KNOWLEDGE_INSCRIBER);
        helper.assertTrue(inscriber != null && inscriber.column() == 6 && inscriber.row() == 0 && inscriber.isSecondary(), "inscriber research is wrong");
        helper.assertTrue(inscriber.parents().equals(List.of(TeResearch.ARCANE_ASSEMBLER)), "inscriber parents are " + inscriber.parents());
        ResearchItem assembler = ResearchCategories.getResearch(TeResearch.ARCANE_ASSEMBLER);
        helper.assertTrue(assembler != null && assembler.column() == 6 && assembler.row() == -1, "assembler research is wrong");
        helper.assertTrue(TcRecipes.get(KnowledgeFeature.CORE_RECIPE) != null && TcRecipes.get(KnowledgeFeature.INSCRIBER_RECIPE) != null, "knowledge recipes are missing");
        helper.succeed();
    }

    private static ArcanePattern pattern(ItemStack result) {
        return new ArcanePattern(List.of(new ItemStack(Items.STICK)), result, new AspectList());
    }

    private static void coreStorage(GameTestHelper helper) {
        ItemStack core = new ItemStack(KnowledgeFeature.CORE.get());
        helper.assertTrue(KnowledgeCore.add(core, pattern(new ItemStack(Items.STONE))), "pattern was not stored");
        helper.assertTrue(!KnowledgeCore.add(core, pattern(new ItemStack(Items.STONE))), "duplicate result was stored");
        for (int index = 1; index < KnowledgeCore.MAXIMUM_STORED_PATTERNS; index++) {
            KnowledgeCore.add(core, pattern(new ItemStack(Items.STONE, index + 1)));
        }
        helper.assertTrue(KnowledgeCore.patterns(core).size() == 21 && !KnowledgeCore.hasRoom(core), "core does not hold 21 patterns");
        helper.assertTrue(!KnowledgeCore.add(core, pattern(new ItemStack(Items.DIRT))), "full core took a pattern");
        helper.assertTrue(KnowledgeCore.remove(core, KnowledgeCore.patternFor(core, new ItemStack(Items.STONE))) && KnowledgeCore.patterns(core).size() == 20, "pattern was not removed");
        helper.succeed();
    }

    private static void inscriberSaveAndDelete(GameTestHelper helper) {
        helper.setBlock(INSCRIBER, KnowledgeFeature.INSCRIBER.get());
        KnowledgeInscriberBlockEntity inscriber = (KnowledgeInscriberBlockEntity) helper.getBlockEntity(INSCRIBER);
        inscriber.setItem(0, new ItemStack(KnowledgeFeature.CORE.get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Knowledge.get(player).addResearch(TeResearch.ARCANE_TERMINAL);
        KnowledgeInscriberMenu menu = new KnowledgeInscriberMenu(1, player.getInventory(), helper.absolutePos(INSCRIBER));
        menu.broadcastChanges();
        helper.assertTrue(menu.saveState() == KnowledgeInscriberMenu.SaveState.INVALID_RECIPE, "empty grid state is " + menu.saveState());
        String[] ids = {"ae2:terminal", "thaumcraft:arcane_worktable", "ae2:calculation_processor"};
        for (int index = 0; index < ids.length; index++) {
            menu.setCarried(item(ids[index]));
            menu.clicked(1 + KnowledgeCore.MAXIMUM_STORED_PATTERNS + index, 0, ClickType.PICKUP, player);
        }
        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(menu.saveState() == KnowledgeInscriberMenu.SaveState.SAVE, "recipe state is " + menu.saveState());
        menu.clickMenuButton(player, KnowledgeInscriberMenu.BUTTON_SAVE_DELETE);
        List<ArcanePattern> stored = KnowledgeCore.patterns(inscriber.getItem(0));
        helper.assertTrue(stored.size() == 1 && stored.get(0).result().is(ActFeature.TERMINAL.get()), "pattern was not saved: " + stored);
        helper.assertTrue(stored.get(0).aspects().size() == 6, "pattern aspects are " + stored.get(0).aspects());
        helper.assertTrue(menu.saveState() == KnowledgeInscriberMenu.SaveState.DELETE && menu.saveCounter() == 1, "state after save is " + menu.saveState());
        menu.clickMenuButton(player, KnowledgeInscriberMenu.BUTTON_CLEAR);
        menu.broadcastChanges();
        menu.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.saveState() == KnowledgeInscriberMenu.SaveState.DELETE, "pattern did not load back");
        menu.clickMenuButton(player, KnowledgeInscriberMenu.BUTTON_SAVE_DELETE);
        helper.assertTrue(KnowledgeCore.patterns(inscriber.getItem(0)).isEmpty(), "pattern was not deleted");
        helper.succeed();
    }
}
