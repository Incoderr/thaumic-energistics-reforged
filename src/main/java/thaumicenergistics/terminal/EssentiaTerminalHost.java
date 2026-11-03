package thaumicenergistics.terminal;

import appeng.api.config.ViewItems;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.storage.ISubMenuHost;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;

public interface EssentiaTerminalHost extends ISubMenuHost {
    @Nullable
    IGrid getEssentiaGrid();

    InternalInventory getWorkInventory();

    @Nullable
    Aspect getSelectedAspect();

    void setSelectedAspect(@Nullable Aspect aspect);

    EssentiaSortMode getSortMode();

    void setSortMode(EssentiaSortMode mode);

    ViewItems getViewMode();

    void setViewMode(ViewItems mode);
}
