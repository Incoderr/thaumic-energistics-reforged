package thaumicenergistics.emitter;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.registry.ModMenus;
import thaumicenergistics.research.TeResearch;
import thaumicenergistics.terminal.TerminalFeature;

public final class EmitterFeature {
    public static final DeferredItem<PartItem<EssentiaLevelEmitterPart>> LEVEL_EMITTER = ModItems.ITEMS.registerItem(
        "essentia_level_emitter", properties -> new PartItem<>(properties, EssentiaLevelEmitterPart.class, EssentiaLevelEmitterPart::new), properties -> properties
    );
    public static final DeferredHolder<MenuType<?>, MenuType<EssentiaLevelEmitterMenu>> MENU = ModMenus.MENUS.register(
        "essentia_level_emitter",
        () -> MenuTypeBuilder.create(EssentiaLevelEmitterMenu::new, EssentiaLevelEmitterPart.class).buildUnregistered(ThaumicEnergistics.id("essentia_level_emitter"))
    );

    private EmitterFeature() {
    }

    public static void load() {
        PartModels.registerModels(EssentiaLevelEmitterPart.MODEL_BASE, EssentiaLevelEmitterPart.MODEL_OFF, EssentiaLevelEmitterPart.MODEL_ON);
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            TerminalFeature.LEVEL_EMITTER_RECIPE, TeResearch.ESSENTIA_TERMINAL, TcStack.of(LEVEL_EMITTER.getId().toString(), 4),
            new AspectList().add(Aspect.FIRE, 4),
            TcIngredient.item("ae2:calculation_processor"),
            TcIngredient.item("minecraft:redstone_torch"),
            TcIngredient.item("thaumcraft:salis_mundus")
        );
    }
}
