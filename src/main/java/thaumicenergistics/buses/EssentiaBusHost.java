package thaumicenergistics.buses;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.util.IConfigurableObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaItems;

public interface EssentiaBusHost extends IUpgradeableObject, IConfigurableObject {
    int FILTER_SIZE = 9;

    @Nullable Aspect getFilter(int index);

    void setFilter(int index, @Nullable Aspect aspect);

    int[] availableFilterSlots();

    boolean supportsVoid();

    boolean isVoidAllowed();

    void setVoidAllowed(boolean allowed);

    boolean isStorageBus();

    boolean isRedstoneControlled();

    void openPriority(Player player);

    default boolean isFilterSlotAvailable(int index) {
        for (int slot : availableFilterSlots()) {
            if (slot == index) {
                return true;
            }
        }
        return false;
    }

    default boolean hasFilter(Aspect aspect) {
        for (int index = 0; index < FILTER_SIZE; index++) {
            if (getFilter(index) == aspect) {
                return true;
            }
        }
        return false;
    }

    default boolean addFilterFromItem(ItemStack stack) {
        Aspect aspect = EssentiaItems.filterAspect(stack);
        if (aspect == null) {
            return false;
        }
        if (hasFilter(aspect)) {
            return true;
        }
        for (int index : availableFilterSlots()) {
            if (getFilter(index) == null) {
                setFilter(index, aspect);
                return true;
            }
        }
        return false;
    }
}
