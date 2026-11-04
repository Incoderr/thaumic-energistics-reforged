package thaumicenergistics.terminal;

import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.ItemMenuHostLocator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

public final class WirelessEssentiaTerminalItem extends WirelessTerminalItem {
    public static final double POWER_STORAGE = 1600000.0;

    public WirelessEssentiaTerminalItem(Properties properties) {
        super(() -> POWER_STORAGE, properties.stacksTo(1));
    }

    @Override
    public MenuType<?> getMenuType() {
        return TerminalFeature.MENU.get();
    }

    @Override
    public WirelessTerminalMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator locator, @Nullable BlockHitResult hit) {
        return new WirelessEssentiaTerminalHost(this, player, locator, (target, subMenu) -> openFromInventory(target, locator, true));
    }
}
