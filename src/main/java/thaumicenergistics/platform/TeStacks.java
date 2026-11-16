package thaumicenergistics.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class TeStacks {
    private TeStacks() {
    }

    public static ItemStack remainder(ItemStack stack) {
        ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }
}
