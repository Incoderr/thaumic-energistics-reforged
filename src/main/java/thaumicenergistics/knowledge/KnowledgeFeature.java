package thaumicenergistics.knowledge;

import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Menus;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModBlockEntities;
import thaumicenergistics.registry.ModBlocks;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;

public final class KnowledgeFeature {
    public static final String CORE_RECIPE = TeRecipes.key("ITEM_KNOWLEDGE_CORE");
    public static final String INSCRIBER_RECIPE = TeRecipes.key("BLOCK_KNOWLEDGE_INSCRIBER");
    public static final String ASSEMBLER_RECIPE = TeRecipes.key("BLOCK_ARCANE_ASSEMBLER");

    public static final TeStackKey<List<ArcanePattern>> PATTERNS = TeStackKey.register(
        "knowledge_core_patterns", ArcanePattern.CODEC.listOf(), ArcanePattern.STREAM_CODEC.apply(ByteBufCodecs.list())
    );
    public static final DeferredItem<KnowledgeCoreItem> CORE = ModItems.ITEMS.registerItem("knowledge_core", KnowledgeCoreItem::new, properties -> properties);
    public static final DeferredBlock<KnowledgeInscriberBlock> INSCRIBER = ModBlocks.withItem(ModBlocks.BLOCKS.registerBlock(
        "knowledge_inscriber", KnowledgeInscriberBlock::new, properties -> properties.strength(1.0F).sound(SoundType.METAL)
    ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnowledgeInscriberBlockEntity>> INSCRIBER_ENTITY = ModBlockEntities.BLOCK_ENTITIES.register(
        "knowledge_inscriber", () -> BlockEntityType.Builder.of(KnowledgeInscriberBlockEntity::new, INSCRIBER.get()).build(null)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<KnowledgeInscriberMenu>> MENU = ModMenus.MENUS.register(
        "knowledge_inscriber", () -> Menus.withPos(KnowledgeInscriberMenu::new)
    );

    private KnowledgeFeature() {
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.arcane(
            CORE_RECIPE, TeResearch.KNOWLEDGE_INSCRIBER, TcStack.of(CORE.getId().toString()),
            new AspectList().add(Aspect.WATER, 3).add(Aspect.ORDER, 3).add(Aspect.EARTH, 1),
            new String[] {"VLV", "LZL", "VCV"},
            'V', TcIngredient.item("ae2:quartz_vibrant_glass"), 'L', TcIngredient.item("minecraft:lapis_lazuli"),
            'Z', TcIngredient.item("thaumcraft:zombie_brain"), 'C', TcIngredient.item("ae2:calculation_processor")
        );
        TeRecipes.arcane(
            INSCRIBER_RECIPE, TeResearch.KNOWLEDGE_INSCRIBER, TcStack.of(INSCRIBER.getId().toString()),
            new AspectList().add(Aspect.WATER, 5).add(Aspect.ORDER, 5).add(Aspect.EARTH, 5).add(Aspect.ENTROPY, 5).add(Aspect.FIRE, 5).add(Aspect.AIR, 5),
            new String[] {"IPI", " T ", "ILI"},
            'I', TcIngredient.tag("c:ingots/iron"),
            'P', TcIngredient.anyOf(TcIngredient.item("ae2:monitor"), TcIngredient.item("ae2:semi_dark_monitor"), TcIngredient.item("ae2:dark_monitor")),
            'T', TcIngredient.item("thaumcraft:thaumonomicon"), 'L', TcIngredient.item("ae2:logic_processor")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.ARCANE_ASSEMBLER,
                new AspectList().add(Aspect.CRAFT, 5).add(Aspect.MECHANISM, 3).add(Aspect.MIND, 3).add(Aspect.EXCHANGE, 3).add(Aspect.AURA, 3).add(Aspect.GREED, 3),
                6, -1, TeResearch.COMPLEXITY_LARGE, TcStack.of("thaumicenergistics:arcane_assembler")
            )
            .setPages(TeResearch.existing(
                TeResearch.text(TeResearch.ARCANE_ASSEMBLER, 1),
                ResearchPage.infusion(ASSEMBLER_RECIPE),
                TeResearch.text(TeResearch.ARCANE_ASSEMBLER, 2),
                TeResearch.text(TeResearch.ARCANE_ASSEMBLER, 3)
            ))
            .setParents(TeResearch.VIS_RELAY_INTERFACE, TeResearch.pseudo(TeResearch.SCEPTRE))
            .setParentsHidden(TeResearch.BASIC)
            .setConcealed()
            .registerResearchItem();
        TeResearch.item(
                TeResearch.KNOWLEDGE_INSCRIBER,
                new AspectList().add(Aspect.MIND, 5).add(Aspect.MECHANISM, 3).add(Aspect.CRAFT, 3).add(Aspect.EXCHANGE, 1),
                6, 0, TeResearch.COMPLEXITY_SMALL, TcStack.of(INSCRIBER.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.KNOWLEDGE_INSCRIBER, 1),
                ResearchPage.arcane(CORE_RECIPE),
                ResearchPage.arcane(INSCRIBER_RECIPE),
                TeResearch.text(TeResearch.KNOWLEDGE_INSCRIBER, 2)
            )
            .setParents(TeResearch.ARCANE_ASSEMBLER)
            .setConcealed()
            .setSecondary()
            .registerResearchItem();
    }
}
