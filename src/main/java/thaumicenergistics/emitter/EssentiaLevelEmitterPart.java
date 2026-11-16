package thaumicenergistics.emitter;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import appeng.parts.automation.PartModelData;
import net.neoforged.neoforge.model.data.ModelData;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigManagerBuilder;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.UpgradeablePart;
import appeng.util.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaKey;

public final class EssentiaLevelEmitterPart extends UpgradeablePart {
    public static final Identifier MODEL_BASE = ThaumicEnergistics.id("part/essentia_level_emitter_base");
    public static final Identifier MODEL_OFF = ThaumicEnergistics.id("part/essentia_level_emitter_off");
    public static final Identifier MODEL_ON = ThaumicEnergistics.id("part/essentia_level_emitter_on");
    public static final long MAX_THRESHOLD = 9999999999L;
    private static final double IDLE_POWER_DRAIN = 0.3;
    private static final String ASPECT_KEY = "aspect";
    private static final String THRESHOLD_KEY = "wantedAmount";
    private static final String EMITTING_KEY = "emitting";

    private @Nullable Aspect trackedAspect;
    private long thresholdLevel;
    private long currentLevel;
    private boolean emitting;
    private @Nullable IStackWatcher watcher;

    public EssentiaLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(IDLE_POWER_DRAIN).addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
            @Override
            public void updateWatcher(IStackWatcher newWatcher) {
                watcher = newWatcher;
                configureWatcher();
            }

            @Override
            public void onStackChange(AEKey what, long amount) {
                if (trackedAspect != null && EssentiaKey.aspectOf(what) == trackedAspect) {
                    setCurrentLevel(amount);
                }
            }
        });
    }

    @Override
    protected void registerSettings(IConfigManagerBuilder builder) {
        super.registerSettings(builder);
        builder.registerSetting(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);
    }

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    public RedstoneMode getRedstoneMode() {
        return getConfigManager().getSetting(Settings.REDSTONE_EMITTER);
    }

    public @Nullable Aspect getTrackedAspect() {
        return trackedAspect;
    }

    public long getThresholdLevel() {
        return thresholdLevel;
    }

    public long getCurrentLevel() {
        return currentLevel;
    }

    public boolean isEmitting() {
        return emitting;
    }

    public void setTrackedAspect(@Nullable Aspect aspect) {
        if (trackedAspect != aspect) {
            trackedAspect = aspect;
            if (!isClientSide()) {
                getHost().markForSave();
                configureWatcher();
            }
        }
    }

    public void setThresholdLevel(long threshold) {
        thresholdLevel = Math.max(0, Math.min(MAX_THRESHOLD, threshold));
        getHost().markForSave();
        updateEmittingState();
    }

    public void adjustThresholdLevel(long amount) {
        setThresholdLevel(thresholdLevel + amount);
    }

    private void configureWatcher() {
        boolean set = false;
        if (watcher != null) {
            watcher.reset();
            if (trackedAspect != null) {
                EssentiaKey key = EssentiaKey.of(trackedAspect);
                watcher.add(key);
                IGrid grid = getMainNode().getGrid();
                if (grid != null) {
                    setCurrentLevel(grid.getStorageService().getCachedInventory().get(key));
                    set = true;
                }
            }
        }
        if (!set) {
            setCurrentLevel(0);
        }
    }

    private void setCurrentLevel(long amount) {
        if (amount != currentLevel) {
            currentLevel = amount;
            updateEmittingState();
        }
    }

    private void updateEmittingState() {
        if (isClientSide() || !getMainNode().isActive()) {
            return;
        }
        boolean emit = switch (getRedstoneMode()) {
            case HIGH_SIGNAL -> currentLevel >= thresholdLevel;
            case LOW_SIGNAL -> currentLevel < thresholdLevel;
            default -> false;
        };
        if (emit != emitting) {
            emitting = emit;
            getHost().markForSave();
            getHost().markForUpdate();
            BlockEntity host = getHost().getBlockEntity();
            Platform.notifyBlocksOfNeighbors(host.getLevel(), host.getBlockPos());
            Platform.notifyBlocksOfNeighbors(host.getLevel(), host.getBlockPos().relative(getSide()));
        }
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        if (getMainNode().hasGridBooted()) {
            updateEmittingState();
        }
    }

    @Override
    protected void onSettingChanged(IConfigManager manager, Setting<?> setting) {
        updateEmittingState();
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return emitting ? 15 : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return isProvidingStrongPower();
    }

    @Override
    public int getLightLevel() {
        return emitting ? 7 : 0;
    }

    @Override
    public void animateTick(Level level, BlockPos pos, RandomSource random) {
        if (emitting) {
            Direction side = getSide();
            double x = side.getStepX() * 0.45F + (random.nextFloat() - 0.5F) * 0.2;
            double y = side.getStepY() * 0.45F + (random.nextFloat() - 0.5F) * 0.2;
            double z = side.getStepZ() * 0.45F + (random.nextFloat() - 0.5F) * 0.2;
            level.addParticle(DustParticleOptions.REDSTONE, 0.5 + pos.getX() + x, 0.5 + pos.getY() + y, 0.5 + pos.getZ() + z, 0, 0, 0);
        }
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(EmitterFeature.MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(7, 7, 11, 9, 9, 15);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 8;
    }


    @Override
    public void collectModelData(ModelData.Builder builder) {
        super.collectModelData(builder);
        builder.with(PartModelData.LEVEL_EMITTER_ON, emitting);
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(emitting);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean old = emitting;
        emitting = data.readBoolean();
        return changed || old != emitting;
    }

    @Override
    public void readFromNBT(ValueInput tag) {
        super.readFromNBT(tag);
        trackedAspect = tag.getString(ASPECT_KEY).map(Aspect::getAspect).orElse(null);
        thresholdLevel = tag.getLongOr(THRESHOLD_KEY, 0L);
        emitting = tag.getBooleanOr(EMITTING_KEY, false);
    }

    @Override
    public void writeToNBT(ValueOutput tag) {
        super.writeToNBT(tag);
        if (trackedAspect != null) {
            tag.putString(ASPECT_KEY, trackedAspect.getTag());
        }
        if (thresholdLevel > 0) {
            tag.putLong(THRESHOLD_KEY, thresholdLevel);
        }
        if (emitting) {
            tag.putBoolean(EMITTING_KEY, true);
        }
    }
}
