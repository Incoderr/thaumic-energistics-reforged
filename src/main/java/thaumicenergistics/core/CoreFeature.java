package thaumicenergistics.core;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;

public final class CoreFeature {
    public static final String COALESCENCE_CORE_RECIPE = TeRecipes.key("MATERIAL_COALESCENCE_CORE");
    public static final String DIFFUSION_CORE_RECIPE = TeRecipes.key("MATERIAL_DIFFUSION_CORE");

    public static final DeferredItem<Item> DIFFUSION_CORE = ModItems.ITEMS.registerItem("diffusion_core", Item::new, properties -> properties);
    public static final DeferredItem<Item> COALESCENCE_CORE = ModItems.ITEMS.registerItem("coalescence_core", Item::new, properties -> properties);
    public static final DeferredItem<Item> IRON_GEAR = ModItems.ITEMS.registerItem("iron_gear", Item::new, properties -> properties);

    private CoreFeature() {
    }

    public static void load() {
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            COALESCENCE_CORE_RECIPE, TeResearch.CORES, TcStack.of(COALESCENCE_CORE.getId().toString()),
            new AspectList().add(Aspect.WATER, 2).add(Aspect.ORDER, 2),
            TcIngredient.item("thaumcraft:quicksilver_drop"), TcIngredient.item("thaumcraft:quicksilver_drop"), TcIngredient.item("thaumcraft:quicksilver_drop"),
            TcIngredient.item("thaumcraft:order_shard"), TcIngredient.item("ae2:formation_core")
        );
        TeRecipes.arcaneShapeless(
            DIFFUSION_CORE_RECIPE, TeResearch.CORES, TcStack.of(DIFFUSION_CORE.getId().toString()),
            new AspectList().add(Aspect.WATER, 2).add(Aspect.ENTROPY, 2),
            TcIngredient.item("thaumcraft:quicksilver_drop"), TcIngredient.item("thaumcraft:quicksilver_drop"), TcIngredient.item("thaumcraft:quicksilver_drop"),
            TcIngredient.item("thaumcraft:entropy_shard"), TcIngredient.item("ae2:annihilation_core")
        );
    }

    public static void research() {
        TeResearch.init();
        new ResearchItem(TeResearch.BASIC, TeResearch.CATEGORY, new AspectList(), 0, 0, 0, ThaumicEnergistics.id("textures/research/tab.icon.png"))
            .setPages(TeResearch.text(TeResearch.BASIC, 1))
            .setRound()
            .setAutoUnlock()
            .registerResearchItem();
        TeResearch.item(
                TeResearch.CORES,
                new AspectList().add(Aspect.SLIME, 3).add(Aspect.MAGIC, 5).add(Aspect.MECHANISM, 5).add(Aspect.EXCHANGE, 5),
                0, -2, TeResearch.COMPLEXITY_MEDIUM, TcStack.of(COALESCENCE_CORE.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.CORES, 1),
                TeResearch.text(TeResearch.CORES, 2),
                ResearchPage.arcane(COALESCENCE_CORE_RECIPE),
                ResearchPage.arcane(DIFFUSION_CORE_RECIPE)
            )
            .setParents(TeResearch.BASIC, TeResearch.pseudo(TeResearch.DISTILESSENTIA))
            .setParentsHidden(TeResearch.DISTILESSENTIA)
            .registerResearchItem();
    }
}
