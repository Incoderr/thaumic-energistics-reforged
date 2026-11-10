package thaumicenergistics.client.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.world.item.ItemStack;
import thaumcraft.compat.rei.ArcaneDisplay;
import thaumicenergistics.act.ActFillGridPayload;
import thaumicenergistics.act.ArcaneCraftingTermMenu;
import thaumicenergistics.client.platform.TeClientNetwork;

@REIPluginClient
public final class ThaumicEnergisticsReiClientPlugin implements REIClientPlugin {
    private static final int GRID = 3;

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register(new ActTransferHandler());
    }

    private static final class ActTransferHandler implements TransferHandler {
        @Override
        public double getPriority() {
            return 100;
        }

        @Override
        public Result handle(Context context) {
            if (!(context.getMenu() instanceof ArcaneCraftingTermMenu)) {
                return Result.createNotApplicable();
            }
            Display display = context.getDisplay();
            int width;
            if (display instanceof ArcaneDisplay arcane) {
                width = arcane.view().width();
            } else if (display instanceof SimpleGridMenuDisplay grid) {
                width = grid.getInputWidth(GRID, GRID);
            } else {
                return Result.createNotApplicable();
            }
            if (width <= 0 || width > GRID) {
                return Result.createNotApplicable();
            }
            List<EntryIngredient> inputs = display.getInputEntries();
            List<List<ItemStack>> slots = new ArrayList<>();
            for (int index = 0; index < ActFillGridPayload.GRID_SIZE; index++) {
                slots.add(new ArrayList<>());
            }
            boolean any = false;
            for (int index = 0; index < inputs.size(); index++) {
                int slot = index / width * GRID + index % width;
                if (slot >= ActFillGridPayload.GRID_SIZE) {
                    return Result.createNotApplicable();
                }
                List<ItemStack> options = slots.get(slot);
                for (EntryStack<?> entry : inputs.get(index)) {
                    if (entry.getType() == VanillaEntryTypes.ITEM && options.size() < ActFillGridPayload.MAX_OPTIONS) {
                        ItemStack stack = entry.<ItemStack>castValue().copyWithCount(1);
                        if (!stack.isEmpty()) {
                            options.add(stack);
                            any = true;
                        }
                    }
                }
            }
            if (!any) {
                return Result.createNotApplicable();
            }
            if (context.isActuallyCrafting()) {
                context.getMinecraft().setScreen(context.getContainerScreen());
                TeClientNetwork.sendToServer(new ActFillGridPayload(slots));
            }
            return Result.createSuccessful().blocksFurtherHandling();
        }
    }
}
