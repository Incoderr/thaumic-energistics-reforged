package thaumicenergistics.buses;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaNetwork;
import thaumicenergistics.essentia.EssentiaTileContainers;

public final class EssentiaImportBusPart extends EssentiaIOBusPart {
    public static final ResourceLocation MODEL_BASE = ThaumicEnergistics.id("part/essentia_import_bus_base");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/essentia_import_bus_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/essentia_import_bus_on");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);

    public EssentiaImportBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public boolean aspectTransferAllowed(@Nullable Aspect aspect) {
        if (aspect == null) {
            return false;
        }
        boolean noFilters = true;
        for (Aspect filter : filters) {
            if (filter != null) {
                if (filter == aspect) {
                    return true;
                }
                noFilters = false;
            }
        }
        return noFilters;
    }

    @Override
    protected boolean doWork(IGrid grid, int transferAmount) {
        AspectContainer container = facingContainer();
        if (container == null) {
            return false;
        }
        Aspect aspect = EssentiaTileContainers.mainAspect(container);
        if (!aspectTransferAllowed(aspect)) {
            return false;
        }
        int drained = EssentiaTileContainers.extract(container, transferAmount, aspect, false);
        if (drained <= 0) {
            return false;
        }
        long accepted = EssentiaNetwork.insert(grid, aspect, drained, Actionable.SIMULATE, source);
        if (accepted <= 0) {
            return false;
        }
        long inserted = EssentiaNetwork.insert(grid, aspect, accepted, Actionable.MODULATE, source);
        EssentiaTileContainers.extract(container, (int) inserted, aspect, true);
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(4, 4, 14, 12, 12, 16);
        helper.addBox(5, 5, 12, 11, 11, 14);
        helper.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public IPartModel getStaticModels() {
        return isActive() ? MODELS_ON : MODELS_OFF;
    }
}
