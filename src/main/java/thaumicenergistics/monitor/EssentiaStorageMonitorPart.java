package thaumicenergistics.monitor;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import appeng.util.InteractionUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.common.util.FakePlayer;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.essentia.EssentiaContainers;
import thaumicenergistics.essentia.EssentiaItems;
import thaumicenergistics.essentia.EssentiaKey;

public class EssentiaStorageMonitorPart extends AEBasePart {
    public static final ResourceLocation MODEL_BASE = ThaumicEnergistics.id("part/essentia_monitor_base");
    public static final ResourceLocation MODEL_EMPTY_OFF = ThaumicEnergistics.id("part/essentia_monitor_empty_off");
    public static final ResourceLocation MODEL_EMPTY_ON = ThaumicEnergistics.id("part/essentia_monitor_empty_on");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/essentia_storage_monitor_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/essentia_storage_monitor_on");
    public static final int ACTIVE_LIGHT_LEVEL = 9;
    private static final double IDLE_DRAIN = 0.0625;
    private static final String LOCKED_KEY = "Locked";
    private static final String TRACKED_KEY = "TrackedAspect";

    public static @Nullable Renderer renderer;

    public interface Renderer {
        void render(EssentiaStorageMonitorPart part, PoseStack poseStack, MultiBufferSource buffers, int light);
    }

    private final IPartModel modelsOff;
    private final IPartModel modelsOn;
    private final IPartModel modelsEmptyOff;
    private final IPartModel modelsEmptyOn;
    private @Nullable Aspect trackedAspect;
    private long trackedAmount;
    private boolean locked;
    private @Nullable IStackWatcher watcher;

    public EssentiaStorageMonitorPart(IPartItem<?> partItem) {
        this(partItem, MODEL_OFF, MODEL_ON);
    }

    protected EssentiaStorageMonitorPart(IPartItem<?> partItem, ResourceLocation off, ResourceLocation on) {
        super(partItem);
        modelsOff = new PartModel(MODEL_BASE, off);
        modelsOn = new PartModel(MODEL_BASE, on);
        modelsEmptyOff = new PartModel(MODEL_BASE, off, MODEL_EMPTY_OFF);
        modelsEmptyOn = new PartModel(MODEL_BASE, on, MODEL_EMPTY_ON);
        getMainNode().setIdlePowerUsage(IDLE_DRAIN).addService(IStorageWatcherNode.class, new IStorageWatcherNode() {
            @Override
            public void updateWatcher(IStackWatcher newWatcher) {
                watcher = newWatcher;
                configureWatcher();
            }

            @Override
            public void onStackChange(AEKey what, long amount) {
                if (trackedAspect != null && EssentiaKey.aspectOf(what) == trackedAspect && trackedAmount != amount) {
                    trackedAmount = amount;
                    getHost().markForUpdate();
                }
            }
        });
    }

    public @Nullable Aspect getTrackedAspect() {
        return trackedAspect;
    }

    public long getTrackedAmount() {
        return trackedAmount;
    }

    public boolean isLocked() {
        return locked;
    }

    protected void track(@Nullable Aspect aspect) {
        trackedAspect = aspect;
        trackedAmount = 0;
        configureWatcher();
        getHost().markForUpdate();
        getHost().markForSave();
    }

    private void configureWatcher() {
        if (watcher == null) {
            return;
        }
        watcher.reset();
        if (trackedAspect != null) {
            EssentiaKey key = EssentiaKey.of(trackedAspect);
            watcher.add(key);
            IGrid grid = getMainNode().getGrid();
            if (grid != null) {
                trackedAmount = Math.max(0, grid.getStorageService().getCachedInventory().get(key));
            }
        }
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        getHost().markForUpdate();
    }

    protected boolean activationCheck(Player player) {
        return !(player instanceof FakePlayer) && isActive();
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }
        if (!activationCheck(player)) {
            return false;
        }
        return onActivatedWithEmptyHand();
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }
        if (!activationCheck(player)) {
            return false;
        }
        if (!player.isShiftKeyDown() && InteractionUtil.canWrenchRotate(heldItem)) {
            locked = !locked;
            player.displayClientMessage(Component.translatable(locked ? "chat.ae2.isNowLocked" : "chat.ae2.isNowUnlocked"), false);
            getHost().markForUpdate();
            getHost().markForSave();
            return true;
        }
        if (player.isShiftKeyDown()) {
            return onShiftActivate(player, heldItem);
        }
        if (isAspectItem(heldItem)) {
            return onActivateWithAspectItem(player, hand, heldItem);
        }
        return false;
    }

    protected static boolean isAspectItem(ItemStack stack) {
        return EssentiaContainers.isContainer(stack) || EssentiaContainers.isLabel(stack) || EssentiaItems.filterAspect(stack) != null;
    }

    protected boolean onShiftActivate(Player player, ItemStack heldItem) {
        return false;
    }

    protected boolean onActivatedWithEmptyHand() {
        if (locked) {
            return false;
        }
        track(null);
        return true;
    }

    protected boolean onActivateWithAspectItem(Player player, InteractionHand hand, ItemStack heldItem) {
        if (locked) {
            return false;
        }
        Aspect aspect = EssentiaItems.filterAspect(heldItem);
        if (aspect == null) {
            return onActivatedWithEmptyHand();
        }
        track(aspect);
        return true;
    }

    @Override
    public boolean requireDynamicRender() {
        return true;
    }

    @Override
    public void renderDynamic(float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int combinedLight, int combinedOverlay) {
        if (renderer != null && isActive() && trackedAspect != null) {
            renderer.render(this, poseStack, buffers, combinedLight);
        }
    }

    @Override
    public IPartModel getStaticModels() {
        boolean active = isActive();
        if (trackedAspect == null) {
            return active ? modelsEmptyOn : modelsEmptyOff;
        }
        return active ? modelsOn : modelsOff;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(2, 2, 14, 14, 14, 16);
        helper.addBox(5, 5, 13, 11, 11, 14);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 3;
    }

    @Override
    public int getLightLevel() {
        return isActive() ? ACTIVE_LIGHT_LEVEL : 0;
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(locked);
        data.writeUtf(trackedAspect == null ? "" : trackedAspect.getTag());
        data.writeVarLong(trackedAmount);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean oldLocked = locked;
        Aspect oldAspect = trackedAspect;
        long oldAmount = trackedAmount;
        locked = data.readBoolean();
        String tag = data.readUtf();
        trackedAspect = tag.isEmpty() ? null : Aspect.getAspect(tag);
        trackedAmount = data.readVarLong();
        return changed || oldLocked != locked || oldAspect != trackedAspect || oldAmount != trackedAmount;
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        locked = tag.getBoolean(LOCKED_KEY);
        trackedAspect = tag.contains(TRACKED_KEY) ? Aspect.getAspect(tag.getString(TRACKED_KEY)) : null;
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        if (locked) {
            tag.putBoolean(LOCKED_KEY, true);
        }
        if (trackedAspect != null) {
            tag.putString(TRACKED_KEY, trackedAspect.getTag());
        }
    }

    protected static void give(Player player, ItemStack stack, Consumer<ItemStack> overflow) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            overflow.accept(stack);
        }
    }
}
