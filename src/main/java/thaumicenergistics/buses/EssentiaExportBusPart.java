package thaumicenergistics.buses;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectContainer;
import thaumcraft.essentia.VoidJarBlockEntity;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaNetwork;
import thaumicenergistics.essentia.EssentiaTileContainers;

public final class EssentiaExportBusPart extends EssentiaIOBusPart {
    public static final ResourceLocation MODEL_BASE = ThaumicEnergistics.id("part/essentia_export_bus_base");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/essentia_export_bus_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/essentia_export_bus_on");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);
    private static final String VOID_KEY = "IsVoidAllowed";

    private boolean voidAllowed;

    public EssentiaExportBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    protected boolean doWork(IGrid grid, int transferAmount) {
        AspectContainer container = facingContainer();
        if (container == null) {
            return false;
        }
        for (Aspect aspect : filters) {
            if (aspect == null) {
                continue;
            }
            if (EssentiaTileContainers.inject(container, 1, aspect, false) <= 0
                && !(voidAllowed && EssentiaTileContainers.mainAspect(container) == aspect)) {
                continue;
            }
            long extracted = EssentiaNetwork.extract(grid, aspect, transferAmount, Actionable.SIMULATE, source);
            if (extracted <= 0) {
                continue;
            }
            boolean voiding = voidAllowed && container instanceof VoidJarBlockEntity;
            long filled = voiding ? extracted : EssentiaTileContainers.inject(container, (int) extracted, aspect, false);
            if (filled <= 0) {
                continue;
            }
            long taken = EssentiaNetwork.extract(grid, aspect, filled, Actionable.MODULATE, source);
            int actual = EssentiaTileContainers.inject(container, (int) taken, aspect, true);
            if (!voiding && actual < taken) {
                EssentiaNetwork.insert(grid, aspect, taken - actual, Actionable.MODULATE, source);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsVoid() {
        return true;
    }

    @Override
    public boolean isVoidAllowed() {
        return voidAllowed;
    }

    @Override
    public void setVoidAllowed(boolean allowed) {
        voidAllowed = allowed;
        getHost().markForSave();
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(4, 4, 12, 12, 12, 13.5);
        helper.addBox(5, 5, 13.5, 11, 11, 15);
        helper.addBox(6, 6, 15, 10, 10, 16);
    }

    @Override
    public IPartModel getStaticModels() {
        return isActive() ? MODELS_ON : MODELS_OFF;
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        voidAllowed = tag.getBoolean(VOID_KEY);
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        if (voidAllowed) {
            tag.putBoolean(VOID_KEY, true);
        }
    }
}
