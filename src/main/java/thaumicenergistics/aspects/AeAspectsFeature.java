package thaumicenergistics.aspects;

import appeng.recipes.AERecipeTypes;
import appeng.recipes.handlers.InscriberRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectRegistry;
import thaumicenergistics.cells.CellsFeature;
import thaumicenergistics.cells.EssentiaStorageType;
import thaumicenergistics.gearbox.GearBoxFeature;

public final class AeAspectsFeature {
    private static final String AE2 = "ae2";
    private static final int CRYSTAL_BASE = 2;
    private static final int FLUIX_CHARGE = 6;
    private static final int MAX_ASPECTS = 6;
    private static final String[] CABLE_COLORS = {
        "fluix", "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private AeAspectsFeature() {
    }

    public static void load() {
    }

    public static void aspects(AspectRegistry registry) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        new Builder(registry, server).run();
        registerTeItems(registry);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(AE2, path));
    }

    private static AspectList list(Object... pairs) {
        AspectList list = new AspectList();
        for (int index = 0; index < pairs.length; index += 2) {
            list.add((Aspect) pairs[index], (Integer) pairs[index + 1]);
        }
        return list;
    }

    private static void registerTeItems(AspectRegistry registry) {
        AspectList aeCell = registry.getObjectTags(item("item_storage_cell_1k"));
        if (aeCell != null && !aeCell.isEmpty()) {
            AspectList cellAspects = aeCell.copy();
            int count = cellAspects.size();
            if (count > 4) {
                List<Aspect> ordered = cellAspects.getAspectsSortedAmount();
                cellAspects.remove(ordered.get(count - 1));
                if (count > 5) {
                    cellAspects.remove(ordered.get(count - 2));
                }
            }
            cellAspects.add(Aspect.MAGIC, 3).add(Aspect.AURA, 5);
            for (EssentiaStorageType type : EssentiaStorageType.values()) {
                if (!type.isCreative()) {
                    registry.register(CellsFeature.cell(type).get(), cellAspects);
                }
            }
        }
        AspectList ironGearbox = list(Aspect.METAL, 7, Aspect.MECHANISM, 5, Aspect.EARTH, 2, Aspect.ENTROPY, 2);
        registry.register(GearBoxFeature.IRON_GEAR_BOX.asItem(), ironGearbox);
        registry.register(GearBoxFeature.THAUMIUM_GEAR_BOX.asItem(), ironGearbox.copy().add(Aspect.MAGIC, 10).add(Aspect.MECHANISM, 5).add(Aspect.METAL, 3));
    }

    private static final class Builder {
        private final AspectRegistry registry;
        private final MinecraftServer server;
        private final Map<Item, List<Recipe<?>>> recipes = new HashMap<>();
        private final Set<Item> done = new HashSet<>();
        private final Set<Item> chain = new HashSet<>();

        private Builder(AspectRegistry registry, MinecraftServer server) {
            this.registry = registry;
            this.server = server;
        }

        private void run() {
            collect(AERecipeTypes.INSCRIBER);
            collect(RecipeType.CRAFTING);
            registerBase();
            for (Item item : BuiltInRegistries.ITEM) {
                if (isAe(item)) {
                    build(item);
                }
            }
        }

        private static boolean isAe(Item item) {
            return AE2.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
        }

        private void collect(RecipeType<?> type) {
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                if (holder.value().getType() != type) {
                    continue;
                }
                ItemStack result = holder.value().getResultItem(server.registryAccess());
                if (!result.isEmpty()) {
                    recipes.computeIfAbsent(result.getItem(), key -> new ArrayList<>()).add(holder.value());
                }
            }
        }

        private void fixed(String path, AspectList aspects) {
            Item item = item(path);
            if (item == Items.AIR) {
                return;
            }
            done.add(item);
            registry.register(item, trim(aspects));
        }

        private void registerBase() {
            fixed("certus_quartz_crystal", list(Aspect.CRYSTAL, CRYSTAL_BASE));
            fixed("charged_certus_quartz_crystal", list(Aspect.CRYSTAL, CRYSTAL_BASE, Aspect.ENERGY, 4));
            fixed("fluix_crystal", list(Aspect.CRYSTAL, CRYSTAL_BASE, Aspect.ENERGY, FLUIX_CHARGE));
            fixed("ender_dust", list(Aspect.ELDRITCH, 2, Aspect.MOTION, 2, Aspect.MAGIC, 1, Aspect.ENTROPY, 1));
            fixed("silicon", list(Aspect.FIRE, 1, Aspect.SENSES, 1, Aspect.ORDER, 1));
            AspectList skyStone = list(Aspect.ELDRITCH, 2, Aspect.EARTH, 1, Aspect.DARKNESS, 1);
            fixed("sky_stone_block", skyStone.copy().add(Aspect.DARKNESS, 3));
            fixed("smooth_sky_stone_block", skyStone);
            fixed("sky_stone_brick", skyStone);
            fixed("sky_stone_small_brick", skyStone);
            fixed("matter_ball", list(Aspect.SLIME, 4, Aspect.EARTH, 4, Aspect.FLIGHT, 4));
            fixed("facade", list(Aspect.METAL, 1));
            AspectList cable = list(Aspect.CRYSTAL, 1, Aspect.ENERGY, 2);
            AspectList covered = cable.copy().add(Aspect.CLOTH, 1);
            AspectList smart = covered.copy().add(Aspect.LIGHT, 1).add(Aspect.ENERGY, 1);
            AspectList dense = covered.copy().add(Aspect.LIGHT, 2).add(Aspect.ENERGY, 3);
            for (String color : CABLE_COLORS) {
                fixed(color + "_glass_cable", cable);
                fixed(color + "_covered_cable", covered);
                fixed(color + "_smart_cable", smart);
                fixed(color + "_covered_dense_cable", dense);
                fixed(color + "_smart_dense_cable", dense);
            }
            fixed("cable_anchor", list(Aspect.METAL, 1));
            AspectList p2p = list(Aspect.METAL, 6, Aspect.ENERGY, 6, Aspect.CRYSTAL, 4, Aspect.GREED, 4, Aspect.ORDER, 4, Aspect.SENSES, 4);
            fixed("me_p2p_tunnel", p2p);
            fixed("item_p2p_tunnel", p2p);
            fixed("fluid_p2p_tunnel", p2p.copy().add(Aspect.WATER, 4));
            fixed("light_p2p_tunnel", p2p.copy().add(Aspect.LIGHT, 4));
            AspectList powered = p2p.copy().add(Aspect.ENERGY, 2);
            fixed("redstone_p2p_tunnel", powered);
            fixed("fe_p2p_tunnel", powered);
            fixed("singularity", list(Aspect.DARKNESS, 10, Aspect.VOID, 10, Aspect.ENTROPY, 10));
            fixed("quantum_entangled_singularity", list(Aspect.ENERGY, 8, Aspect.EXCHANGE, 8, Aspect.MOTION, 4, Aspect.ELDRITCH, 4));
            AspectList pattern = list(Aspect.CRAFT, 3, Aspect.MIND, 3);
            fixed("crafting_pattern", pattern);
            fixed("processing_pattern", pattern);
            fixed("smithing_table_pattern", pattern);
            fixed("stonecutting_pattern", pattern);
        }

        private AspectList tags(Item item) {
            if (isAe(item) && !chain.contains(item)) {
                build(item);
            }
            return registry.getObjectTags(item);
        }

        private void build(Item item) {
            if (done.contains(item)) {
                return;
            }
            if (registry.hasAspects(item)) {
                done.add(item);
                return;
            }
            chain.add(item);
            AspectList ingredients = new AspectList();
            for (Recipe<?> recipe : recipes.getOrDefault(item, List.of())) {
                AspectList candidate = fromRecipe(recipe);
                if (!candidate.isEmpty()) {
                    ingredients = candidate;
                    break;
                }
            }
            AspectList bonus = new AspectList();
            bonuses(BuiltInRegistries.ITEM.getKey(item).getPath(), ingredients, bonus);
            chain.remove(item);
            done.add(item);
            AspectList result = ingredients.copy().add(bonus);
            if (!result.isEmpty()) {
                registry.register(item, trim(result));
            }
        }

        private AspectList fromRecipe(Recipe<?> recipe) {
            List<Ingredient> inputs = new ArrayList<>();
            if (recipe instanceof InscriberRecipe inscriber) {
                inputs.add(inscriber.getMiddleInput());
                inputs.add(inscriber.getTopOptional());
                inputs.add(inscriber.getBottomOptional());
            } else if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
                inputs.addAll(recipe.getIngredients());
            }
            AspectList result = new AspectList();
            for (Ingredient input : inputs) {
                if (input == null || input.isEmpty()) {
                    continue;
                }
                ItemStack[] options = input.getItems();
                if (options.length == 0) {
                    continue;
                }
                AspectList stackAspects = tags(options[0].getItem());
                if (stackAspects == null) {
                    continue;
                }
                for (Aspect aspect : stackAspects.getAspects()) {
                    int existing = result.getAmount(aspect);
                    int amount = stackAspects.getAmount(aspect);
                    if (existing < amount) {
                        result.add(aspect, amount - existing);
                    }
                }
            }
            return result;
        }

        private static void bonuses(String path, AspectList ingredients, AspectList bonus) {
            switch (path) {
                case "molecular_assembler" -> bonus.add(Aspect.CRAFT, 4).add(Aspect.ENERGY, 6).add(Aspect.MECHANISM, 2).add(Aspect.CRYSTAL, 1);
                case "sky_stone_chest" -> {
                    bonus.add(Aspect.VOID, 4);
                    ingredients.remove(Aspect.EARTH);
                }
                case "quartz_glass" -> bonus.add(Aspect.CRYSTAL, 4);
                case "controller" -> {
                    bonus.add(Aspect.MECHANISM, 3).add(Aspect.SENSES, 1).add(Aspect.MIND, 9);
                    ingredients.remove(Aspect.FIRE);
                    ingredients.remove(Aspect.ORDER);
                }
                case "drive", "chest", "io_port" -> {
                    bonus.add("io_port".equals(path) ? Aspect.EXCHANGE : Aspect.VOID, 4);
                    ingredients.remove(Aspect.FIRE);
                    ingredients.remove(Aspect.ORDER);
                    ingredients.remove(Aspect.SENSES);
                    ingredients.remove(Aspect.MECHANISM);
                }
                case "condenser" -> bonus.add(Aspect.VOID, 10);
                case "redstone_card" -> bonus.add(Aspect.SENSES, 2);
                case "cell_workbench" -> bonus.add(Aspect.CLOTH, 2);
                case "interface" -> bonus.add(Aspect.EXCHANGE, 6);
                case "logic_processor" -> processor(ingredients, bonus, 3, true);
                case "calculation_processor" -> processor(ingredients, bonus, 6, true);
                case "engineering_processor" -> processor(ingredients, bonus, 9, false);
                default -> {
                }
            }
        }

        private static void processor(AspectList ingredients, AspectList bonus, int mind, boolean metal) {
            ingredients.remove(Aspect.METAL);
            ingredients.remove(Aspect.FIRE);
            ingredients.remove(Aspect.ORDER);
            bonus.add(Aspect.MIND, mind);
            if (metal) {
                bonus.add(Aspect.METAL, 1);
            }
        }

        private static AspectList trim(AspectList aspects) {
            AspectList result = aspects.copy();
            List<Aspect> sorted = result.getAspectsSortedAmount();
            for (int index = MAX_ASPECTS; index < sorted.size(); index++) {
                result.remove(sorted.get(index));
            }
            return result;
        }
    }
}
