package thaumicenergistics.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import thaumcraft.recipeview.ArcaneView;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.act.ActFeature;
import thaumicenergistics.act.ActFillGridPayload;
import thaumicenergistics.act.ArcaneCraftingTermMenu;
import thaumicenergistics.client.platform.TeClientNetwork;

@JeiPlugin
public final class ThaumicEnergisticsJeiPlugin implements IModPlugin {
    private static final int GRID = 3;
    private static final IRecipeType<ArcaneView> ARCANE = IRecipeType.create(Identifier.fromNamespaceAndPath("thaumcraft", "arcane"), ArcaneView.class);

    @Override
    public Identifier getPluginUid() {
        return ThaumicEnergistics.id("jei");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new ActTransferHandler<>(helper, RecipeTypes.CRAFTING, recipe -> GRID), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new ActTransferHandler<>(helper, ARCANE, ArcaneView::width), ARCANE);
    }

    private interface GridWidth<R> {
        int width(R recipe);
    }

    private record ActTransferHandler<R>(IRecipeTransferHandlerHelper helper, IRecipeType<R> type, GridWidth<R> gridWidth)
        implements IRecipeTransferHandler<ArcaneCraftingTermMenu, R> {
        @Override
        public Class<? extends ArcaneCraftingTermMenu> getContainerClass() {
            return ArcaneCraftingTermMenu.class;
        }

        @Override
        public Optional<MenuType<ArcaneCraftingTermMenu>> getMenuType() {
            return Optional.of(ActFeature.MENU.get());
        }

        @Override
        public IRecipeType<R> getRecipeType() {
            return type;
        }

        @Override
        @SuppressWarnings("removal")
        public IRecipeTransferError transferRecipe(ArcaneCraftingTermMenu menu, R recipe, IRecipeSlotsView slotsView, Player player, boolean maxTransfer, boolean doTransfer) {
            int width = gridWidth.width(recipe);
            if (width <= 0 || width > GRID) {
                return helper.createInternalError();
            }
            List<IRecipeSlotView> inputs = slotsView.getSlotViews(RecipeIngredientRole.INPUT);
            List<List<ItemStack>> slots = new ArrayList<>();
            for (int index = 0; index < ActFillGridPayload.GRID_SIZE; index++) {
                slots.add(new ArrayList<>());
            }
            boolean any = false;
            for (int index = 0; index < inputs.size(); index++) {
                int slot = index / width * GRID + index % width;
                if (slot >= ActFillGridPayload.GRID_SIZE) {
                    return helper.createInternalError();
                }
                List<ItemStack> options = slots.get(slot);
                for (ItemStack stack : inputs.get(index).getItemStacks().toList()) {
                    if (options.size() >= ActFillGridPayload.MAX_OPTIONS) {
                        break;
                    }
                    if (!stack.isEmpty()) {
                        options.add(stack.copyWithCount(1));
                        any = true;
                    }
                }
            }
            if (!any) {
                return helper.createInternalError();
            }
            if (doTransfer) {
                TeClientNetwork.sendToServer(new ActFillGridPayload(slots));
            }
            return null;
        }
    }
}
