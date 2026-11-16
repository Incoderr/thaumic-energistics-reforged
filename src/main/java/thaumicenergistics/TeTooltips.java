package thaumicenergistics;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumicenergistics.assembler.ArcaneAssemblerBlock;
import thaumicenergistics.assembler.AssemblerFeature;
import thaumicenergistics.vibration.VibrationChamberBlock;
import thaumicenergistics.vibration.VibrationFeature;

@EventBusSubscriber(modid = ThaumicEnergistics.MODID)
public final class TeTooltips {
    private TeTooltips() {
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(AssemblerFeature.BLOCK.asItem())) {
            ArcaneAssemblerBlock.appendTooltip(stack, event.getToolTip());
        } else if (stack.is(VibrationFeature.CHAMBER.asItem())) {
            VibrationChamberBlock.appendTooltip(stack, event.getToolTip());
        }
    }
}
