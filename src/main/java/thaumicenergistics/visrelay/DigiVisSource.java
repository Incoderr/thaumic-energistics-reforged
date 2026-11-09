package thaumicenergistics.visrelay;

import appeng.api.networking.IGrid;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;

/**
 * Digital vis source, inserted into the AE grid.
 */
public interface DigiVisSource {
    int consumeVis(Aspect aspect, int amount);

    /**
     * The AE grid the source is attached to.
     */
    @Nullable
    IGrid getGrid();

    GlobalPos getLocation();

    /**
     * The side of the host the source part is attached to.
     * Null when the source is a whole block.
     */
    @Nullable
    Direction getSide();

    long uid();

    boolean isActive();
}
