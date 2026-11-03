package thaumicenergistics.client.cells;

import appeng.core.localization.GuiText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.item.AspectTooltips;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.cells.EssentiaCellInventory;

@EventBusSubscriber(modid = ThaumicEnergistics.MODID, value = Dist.CLIENT)
public final class EssentiaCellTooltips {
    private EssentiaCellTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        EssentiaCellInventory cell = EssentiaCellInventory.of(stack, null);
        if (cell == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("thaumicenergistics.tooltip.essentia.cell.bytes", cell.getUsedBytes(), cell.getTotalBytes()));
        lines.add(Component.translatable("thaumicenergistics.tooltip.essentia.cell.types", cell.getUsedTypes(), cell.getTotalTypes()));
        if (cell.isPartitioned()) {
            lines.add(GuiText.Partitioned.text());
        }
        if (cell.getUsedTypes() > 0) {
            if (Screen.hasShiftDown()) {
                List<Map.Entry<Aspect, Long>> stored = new ArrayList<>(cell.getStoredEssentia().entrySet());
                stored.sort(Comparator.comparing(entry -> entry.getKey().getName()));
                for (Map.Entry<Aspect, Long> entry : stored) {
                    lines.add(contentLine(entry.getKey(), entry.getValue()));
                }
            } else {
                lines.add(Component.translatable("thaumicenergistics.tooltip.itemstack.details").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
            }
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    private static Component contentLine(Aspect aspect, long amount) {
        MutableComponent name = AspectTooltips.isDiscovered(aspect) ? Component.literal(aspect.getName()) : Component.translatable("tc.aspect.unknown");
        ChatFormatting color = aspect.getChatcolor() == null ? null : ChatFormatting.getByCode(aspect.getChatcolor().charAt(0));
        if (color != null) {
            name.withStyle(color);
        }
        return name.append(Component.literal(" x " + amount).withStyle(ChatFormatting.WHITE));
    }
}
