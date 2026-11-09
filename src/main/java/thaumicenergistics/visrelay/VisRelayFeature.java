package thaumicenergistics.visrelay;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;
import thaumicenergistics.crafting.TeRecipes;
import thaumicenergistics.platform.TeStackKey;
import thaumicenergistics.registry.ModItems;
import thaumicenergistics.research.TeResearch;

public final class VisRelayFeature {
    public static final String RECIPE = TeRecipes.key("PART_VIS_INTERFACE");

    /**
     * Vis relay interface link stored on memory cards.
     */
    public static final TeStackKey<CompoundTag> LINK = TeStackKey.register("vis_source_link", CompoundTag.CODEC, ByteBufCodecs.COMPOUND_TAG);

    public static final DeferredItem<PartItem<VisRelayInterfacePart>> INTERFACE = ModItems.ITEMS.registerItem(
        "vis_relay_interface", properties -> new PartItem<>(properties, VisRelayInterfacePart.class, VisRelayInterfacePart::new), properties -> properties
    );

    private VisRelayFeature() {
    }

    public static void load() {
        PartModels.registerModels(VisRelayInterfacePart.MODEL_BASE, VisRelayInterfacePart.MODEL_OFF, VisRelayInterfacePart.MODEL_ON);
    }

    public static void recipes() {
        TeRecipes.arcaneShapeless(
            RECIPE, TeResearch.VIS_RELAY_INTERFACE, TcStack.of(INTERFACE.getId().toString()),
            new AspectList().add(Aspect.AIR, 2).add(Aspect.EARTH, 2).add(Aspect.ENTROPY, 2).add(Aspect.FIRE, 2).add(Aspect.ORDER, 2).add(Aspect.WATER, 2),
            TcIngredient.item("thaumcraft:balanced_shard"),
            TcIngredient.item("ae2:me_p2p_tunnel")
        );
    }

    public static void research() {
        TeResearch.item(
                TeResearch.VIS_RELAY_INTERFACE,
                new AspectList().add(Aspect.AURA, 5).add(Aspect.ENERGY, 4).add(Aspect.VOID, 3).add(Aspect.MECHANISM, 2),
                4, -1, TeResearch.COMPLEXITY_MEDIUM, TcStack.of(INTERFACE.getId().toString())
            )
            .setPages(
                TeResearch.text(TeResearch.VIS_RELAY_INTERFACE, 1),
                ResearchPage.arcane(RECIPE),
                TeResearch.text(TeResearch.VIS_RELAY_INTERFACE, 2),
                TeResearch.text(TeResearch.VIS_RELAY_INTERFACE, 3)
            )
            .setParents(TeResearch.ARCANE_TERMINAL, TeResearch.pseudo(TeResearch.VISPOWER))
            .registerResearchItem();
    }
}
