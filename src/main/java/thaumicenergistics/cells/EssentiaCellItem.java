package thaumicenergistics.cells;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class EssentiaCellItem extends Item {
    private final EssentiaStorageType storageType;

    public EssentiaCellItem(Properties properties, EssentiaStorageType storageType) {
        super(properties.stacksTo(1).rarity(storageType.rarity()));
        this.storageType = storageType;
    }

    public EssentiaStorageType storageType() {
        return storageType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || storageType.isCreative()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        EssentiaCellInventory inventory = EssentiaCellInventory.of(stack, null);
        if (inventory == null || inventory.getUsedBytes() != 0 || !player.getInventory().add(new ItemStack(CellsFeature.HOUSING.get()))) {
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.success(new ItemStack(CellsFeature.component(storageType).get()));
    }
}
