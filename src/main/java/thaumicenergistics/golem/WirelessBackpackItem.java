package thaumicenergistics.golem;

import appeng.api.ids.AEComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class WirelessBackpackItem extends Item {
    public WirelessBackpackItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isLinked(ItemStack stack) {
        return stack.has(AEComponents.WIRELESS_LINK_TARGET);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isLinked(stack) ? "gui.ae2.Linked" : "gui.ae2.Unlinked"));
    }
}
