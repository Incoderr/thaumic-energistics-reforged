package thaumicenergistics.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipe;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumicenergistics.Config;
import thaumicenergistics.ThaumicEnergistics;

public final class TeRecipes {
    private TeRecipes() {
    }

    public static String key(String name) {
        return ThaumicEnergistics.MODID + "." + name;
    }

    private static final String GATE = "thaumicenergistics_gate";

    static {
        TcStack.registerDataHandler(GATE, (stack, value) -> {
        });
        TcStack.registerDataMatcher(GATE, (stack, value) -> switch (value) {
            case "crucible" -> Config.crucibleAssembler();
            case "infusion" -> Config.infusionAssembler();
            default -> true;
        });
    }

    public static TcIngredient gated(String item, String gate) {
        return TcIngredient.of(TcStack.withData(item, 1, Map.of(GATE, gate)));
    }

    public static <T extends TcRecipe> T register(T recipe) {
        if (TcRecipes.get(recipe.key()) != null) {
            throw new IllegalStateException("Recipe key " + recipe.key() + " is already registered");
        }
        return TcRecipes.register(recipe);
    }

    public static ShapedArcaneRecipe arcane(String key, String research, TcStack result, AspectList aspects, String[] pattern, Object... symbols) {
        List<TcIngredient> grid = new ArrayList<>();
        int width = 0;
        for (String row : pattern) {
            width = Math.max(width, row.length());
        }
        for (String row : pattern) {
            for (int column = 0; column < width; column++) {
                grid.add(column < row.length() ? symbol(row.charAt(column), symbols) : TcIngredient.EMPTY);
            }
        }
        return register(new ShapedArcaneRecipe(key, research, result, aspects, width, pattern.length, grid));
    }

    public static ShapelessArcaneRecipe arcaneShapeless(String key, String research, TcStack result, AspectList aspects, TcIngredient... ingredients) {
        return register(new ShapelessArcaneRecipe(key, research, result, aspects, List.of(ingredients)));
    }

    public static CraftingRecipeRef vanilla(String key, String research, String recipePath, TcStack result) {
        return register(new CraftingRecipeRef(key, research, ThaumicEnergistics.id(recipePath), result));
    }

    private static TcIngredient symbol(char symbol, Object[] symbols) {
        if (symbol == ' ') {
            return TcIngredient.EMPTY;
        }
        for (int index = 0; index + 1 < symbols.length; index += 2) {
            if (symbols[index] instanceof Character key && key == symbol) {
                return (TcIngredient) symbols[index + 1];
            }
        }
        throw new IllegalArgumentException("No ingredient for pattern symbol " + symbol);
    }
}
