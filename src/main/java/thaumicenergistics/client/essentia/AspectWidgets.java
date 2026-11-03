package thaumicenergistics.client.essentia;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.item.AspectTooltips;

public final class AspectWidgets {
    private AspectWidgets() {
    }

    public static void draw(GuiGraphics graphics, int x, int y, Aspect aspect) {
        if (AspectTooltips.isDiscovered(aspect)) {
            AspectRenderer.drawTag(graphics, x, y, aspect, 0.0F, 0, 1.0F, false);
        } else {
            AspectRenderer.drawUnknown(graphics, x, y);
        }
    }

    public static Component name(Aspect aspect) {
        return AspectTooltips.isDiscovered(aspect) ? Component.literal(aspect.getName()) : Component.translatable("tc.aspect.unknown");
    }

    public static List<Component> tooltip(Aspect aspect) {
        List<Component> lines = new ArrayList<>();
        if (AspectTooltips.isDiscovered(aspect)) {
            lines.add(Component.literal(aspect.getName()));
            lines.add(aspect.getDescription().copy().withStyle(ChatFormatting.GRAY));
            String namespace = aspect.getImage().getNamespace();
            String mod = ModList.get().getModContainerById(namespace).map(container -> container.getModInfo().getDisplayName()).orElse("Unknown");
            lines.add(Component.literal(mod).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else {
            Component unknown = Component.translatable("tc.aspect.unknown");
            lines.add(unknown);
            lines.add(unknown.copy().withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Unknown").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
        return lines;
    }
}
