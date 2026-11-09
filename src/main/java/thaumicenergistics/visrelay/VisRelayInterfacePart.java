package thaumicenergistics.visrelay;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.ids.AEComponents;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumcraft.vis.VisRelayBlockEntity;
import thaumicenergistics.ThaumicEnergistics;

/**
 * Interfaces with a vis relay.
 */
public final class VisRelayInterfacePart extends AEBasePart implements IGridTickable, DigiVisSource {
    public static final ResourceLocation MODEL_BASE = ThaumicEnergistics.id("part/vis_relay_interface_base");
    public static final ResourceLocation MODEL_OFF = ThaumicEnergistics.id("part/vis_relay_interface_off");
    public static final ResourceLocation MODEL_ON = ThaumicEnergistics.id("part/vis_relay_interface_on");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON);

    private static final String NBT_KEY_UID = "uid";
    private static final String NBT_KEY_IS_PROVIDER = "isProvider";
    private static final String NBT_KEY_PROVIDER_SOURCE = "linkedSource";

    /**
     * The amount of time to display the color when not receiving updates.
     */
    private static final long TIME_TO_CLEAR = 500;

    /**
     * The amount of power to use per each vis of a request.
     * The amount of vis doesn't matter.
     */
    private static final int POWER_PER_REQUESTED_VIS = 4;

    private static final TickingRequest TICKING = new TickingRequest(30, 30, false);

    public interface Renderer {
        void render(VisRelayInterfacePart part, PoseStack poseStack, MultiBufferSource buffers, int light);
    }

    public static @Nullable Renderer renderer;

    /**
     * Unique ID for this interface.
     */
    private long uid;

    /**
     * The aspect color we are currently draining.
     */
    private int visDrainingColor;

    /**
     * The last time the color was refreshed.
     */
    private long lastColorUpdate;

    /**
     * True if this end of the P2P is a vis provider.
     */
    private boolean isProvider;

    /**
     * If this end is a provider, this stores the source.
     */
    private final VisSourceLink visP2PSourceInfo = new VisSourceLink();

    /**
     * If this end is a provider, this interacts with the vis network.
     */
    private @Nullable VisProviderNode visProviderNode;

    public VisRelayInterfacePart(IPartItem<?> partItem) {
        super(partItem);
        this.uid = UUID.randomUUID().getLeastSignificantBits();
        getMainNode().addService(IGridTickable.class, this);
    }

    /**
     * Requests that the interface drain vis.
     */
    @Override
    public int consumeVis(Aspect aspect, int amount) {
        if (!isActive()) {
            return 0;
        }
        int amountReceived = 0;
        if (isProvider) {
            if (isP2PSourceValid()) {
                DigiVisSource source = visP2PSourceInfo.tryGetSource(getGrid());
                if (source != null) {
                    amountReceived = source.consumeVis(aspect, amount);
                }
            }
        } else {
            amountReceived = consumeVisFromVisNetwork(aspect, amount);
        }
        if (amountReceived > 0) {
            setDrainColor(aspect.getColor());
        }
        return amountReceived;
    }

    @Override
    public @Nullable IGrid getGrid() {
        IGridNode node = getGridNode();
        return node != null ? node.getGrid() : null;
    }

    @Override
    public GlobalPos getLocation() {
        BlockPos pos = getBlockEntity().getBlockPos();
        return GlobalPos.of(getLevel().dimension(), pos);
    }

    @Override
    public long uid() {
        return uid;
    }

    private int consumeVisFromVisNetwork(Aspect aspect, int amount) {
        VisRelayBlockEntity visRelay = getRelay();
        if (visRelay == null) {
            return 0;
        }
        IEnergyService energy = getGrid() != null ? getGrid().getEnergyService() : null;
        if (energy == null) {
            return 0;
        }
        double drainedPower = energy.extractAEPower(POWER_PER_REQUESTED_VIS, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (drainedPower < POWER_PER_REQUESTED_VIS) {
            return 0;
        }
        int amountReceived = visRelay.consumeVis(aspect, amount);
        if (amountReceived > 0) {
            energy.extractAEPower(POWER_PER_REQUESTED_VIS, Actionable.MODULATE, PowerMultiplier.CONFIG);
        }
        return amountReceived;
    }

    /**
     * Verifies that a P2P source is valid.
     */
    private boolean isP2PSourceValid() {
        if (!visP2PSourceInfo.hasSourceData()) {
            return false;
        }
        DigiVisSource p2pSource = visP2PSourceInfo.tryGetSource(getGrid());
        if (!(p2pSource instanceof VisRelayInterfacePart visSource)) {
            return false;
        }
        if (visSource == this) {
            return false;
        }
        return !visSource.isVisProvider();
    }

    /**
     * Sets the color we are draining.
     */
    private void setDrainColor(int color) {
        if (color != 0) {
            if (color == visDrainingColor) {
                lastColorUpdate = System.currentTimeMillis();
                return;
            }
            if (System.currentTimeMillis() - lastColorUpdate <= TIME_TO_CLEAR / 2) {
                return;
            }
            lastColorUpdate = System.currentTimeMillis();
        }
        visDrainingColor = color;
        getHost().markForUpdate();
    }

    private void setIsVisProvider(boolean isProviding) {
        if (!isProviding) {
            visP2PSourceInfo.clearData();
            if (visProviderNode != null) {
                visProviderNode.invalidate();
                visProviderNode = null;
            }
        }
        isProvider = isProviding;
    }

    /**
     * Gets the relay the interface is facing, if any.
     */
    public @Nullable VisRelayBlockEntity getRelay() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        BlockPos pos = getBlockEntity().getBlockPos().relative(getSide());
        if (level.getBlockEntity(pos) instanceof VisRelayBlockEntity relay && relay.getOrientation() == getSide()) {
            return relay;
        }
        return null;
    }

    public boolean isVisProvider() {
        return isProvider;
    }

    /**
     * The color being drained, 0 when idle.
     */
    public int getDrainColor() {
        return visDrainingColor;
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(6.0F, 6.0F, 15.0F, 10.0F, 10.0F, 16.0F);
        helper.addBox(4.0F, 4.0F, 14.0F, 12.0F, 12.0F, 15.0F);
        helper.addBox(5.0F, 5.0F, 13.0F, 11.0F, 11.0F, 14.0F);
    }

    @Override
    public int getLightLevel() {
        return isActive() ? 8 : 0;
    }

    @Override
    public float getCableConnectionLength(AECableType type) {
        return 2;
    }

    @Override
    public IPartModel getStaticModels() {
        return isActive() ? MODELS_ON : MODELS_OFF;
    }

    @Override
    public boolean requireDynamicRender() {
        return true;
    }

    @Override
    public void renderDynamic(float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int combinedLight, int combinedOverlay) {
        if (renderer != null && visDrainingColor != 0) {
            renderer.render(this, poseStack, buffers, combinedLight);
        }
    }

    @Override
    public boolean onUseItemOn(ItemStack held, Player player, InteractionHand hand, Vec3 position) {
        if (isClientSide()) {
            return true;
        }
        if (held.getItem() instanceof IMemoryCard card) {
            if (player.isShiftKeyDown()) {
                return super.onUseItemOn(held, player, hand, position);
            }
            CompoundTag data = VisRelayFeature.LINK.get(held);
            if (data != null) {
                visP2PSourceInfo.readFromTag(data);
                if (visP2PSourceInfo.hasSourceData()) {
                    if (!isP2PSourceValid()) {
                        card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                        visP2PSourceInfo.clearData();
                    } else {
                        setIsVisProvider(true);
                        card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                    }
                }
                getHost().markForSave();
            } else if (isProvider && held.get(AEComponents.EXPORTED_SETTINGS_SOURCE) == null) {
                setIsVisProvider(false);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
                getHost().markForSave();
            }
            return true;
        }
        return super.onUseItemOn(held, player, hand, position);
    }

    @Override
    public void exportSettings(SettingsFrom from, DataComponentMap.Builder builder) {
        super.exportSettings(from, builder);
        if (from == SettingsFrom.MEMORY_CARD) {
            builder.set(VisRelayFeature.LINK.type(), new VisSourceLink(this).writeToTag());
            setIsVisProvider(false);
            getHost().markForSave();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return TICKING;
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (visDrainingColor != 0 && System.currentTimeMillis() - lastColorUpdate > TIME_TO_CLEAR) {
            setDrainColor(0);
        }
        if (isProvider) {
            boolean hasProvider = visProviderNode != null;
            if (!isP2PSourceValid()) {
                if (hasProvider) {
                    visProviderNode.invalidate();
                    visProviderNode = null;
                }
            } else {
                boolean hasRelay = getRelay() != null;
                if (!hasRelay && hasProvider) {
                    visProviderNode.invalidate();
                    visProviderNode = null;
                } else if (hasRelay && !hasProvider) {
                    visProviderNode = VisProviderNode.create(this, getLevel());
                } else if (hasProvider) {
                    visProviderNode.updateVisInterface();
                }
            }
        }
        return TickRateModulation.SAME;
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        tag.putLong(NBT_KEY_UID, uid);
        if (isProvider) {
            tag.putBoolean(NBT_KEY_IS_PROVIDER, isProvider);
            visP2PSourceInfo.writeToTag(tag, NBT_KEY_PROVIDER_SOURCE);
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        if (tag.contains(NBT_KEY_UID)) {
            uid = tag.getLong(NBT_KEY_UID);
        }
        if (tag.contains(NBT_KEY_IS_PROVIDER)) {
            isProvider = tag.getBoolean(NBT_KEY_IS_PROVIDER);
            if (tag.contains(NBT_KEY_PROVIDER_SOURCE)) {
                visP2PSourceInfo.readFromTag(tag, NBT_KEY_PROVIDER_SOURCE);
            }
        }
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeInt(visDrainingColor);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean redraw = super.readFromStream(data);
        int oldColor = visDrainingColor;
        visDrainingColor = data.readInt();
        redraw |= visDrainingColor != oldColor;
        return redraw;
    }
}
