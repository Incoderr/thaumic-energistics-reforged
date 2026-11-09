package thaumicenergistics.visrelay;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartHost;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Identifies a linked digivis source.
 */
public final class VisSourceLink {
    private static final String NBT_KEY_HAS_DATA = "hasData";
    private static final String NBT_KEY_DATA = "data";
    private static final String NBT_KEY_UID = "uid";

    private boolean hasData;
    private ResourceKey<Level> world;
    private BlockPos pos = BlockPos.ZERO;
    private @Nullable Direction side;
    private long uid;
    private WeakReference<DigiVisSource> visSource = new WeakReference<>(null);

    public VisSourceLink() {
        clearData();
    }

    public VisSourceLink(DigiVisSource source) {
        clearData();
        GlobalPos location = source.getLocation();
        this.world = location.dimension();
        this.pos = location.pos();
        this.side = source.getSide();
        this.uid = source.uid();
        this.hasData = true;
    }

    public boolean hasSourceData() {
        return hasData;
    }

    public void clearData() {
        hasData = false;
        world = Level.OVERWORLD;
        pos = BlockPos.ZERO;
        side = null;
        uid = 0;
        visSource = new WeakReference<>(null);
    }

    public CompoundTag writeToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NBT_KEY_HAS_DATA, hasData);
        if (hasData) {
            CompoundTag data = new CompoundTag();
            data.putString("world", world.location().toString());
            data.putLong("pos", pos.asLong());
            data.putInt("side", side == null ? -1 : side.ordinal());
            tag.put(NBT_KEY_DATA, data);
            tag.putLong(NBT_KEY_UID, uid);
        }
        return tag;
    }

    public void writeToTag(CompoundTag data, String name) {
        data.put(name, writeToTag());
    }

    public void readFromTag(@Nullable CompoundTag tag) {
        clearData();
        if (tag == null || !tag.getBoolean(NBT_KEY_HAS_DATA)
            || !tag.contains(NBT_KEY_UID) || !tag.contains(NBT_KEY_DATA)) {
            return;
        }
        CompoundTag data = tag.getCompound(NBT_KEY_DATA);
        ResourceLocation dimension = ResourceLocation.tryParse(data.getString("world"));
        if (dimension == null) {
            return;
        }
        world = ResourceKey.create(Registries.DIMENSION, dimension);
        pos = BlockPos.of(data.getLong("pos"));
        int sideIndex = data.getInt("side");
        side = sideIndex >= 0 && sideIndex < Direction.values().length ? Direction.values()[sideIndex] : null;
        uid = tag.getLong(NBT_KEY_UID);
        hasData = true;
    }

    public void readFromTag(CompoundTag data, String name) {
        readFromTag(data.getCompound(name));
    }

    /**
     * Verifies that the source exists, and that the source and destination grids are the same.
     */
    public @Nullable DigiVisSource tryGetSource(@Nullable IGrid destinationGrid) {
        if (destinationGrid == null) {
            return null;
        }
        DigiVisSource source = tryGetSource(destinationGrid, false);
        if (source == null) {
            source = tryGetSource(destinationGrid, true);
        }
        return source;
    }

    private @Nullable DigiVisSource tryGetSource(IGrid destinationGrid, boolean forceUpdate) {
        DigiVisSource source = getSource(forceUpdate);
        if (source != null) {
            IGrid sourceGrid = source.getGrid();
            if (sourceGrid == null || !destinationGrid.equals(sourceGrid)) {
                return null;
            }
        }
        return source;
    }

    private @Nullable DigiVisSource getSource(boolean forceUpdate) {
        if (hasData) {
            DigiVisSource cached = visSource.get();
            if (forceUpdate || cached == null) {
                visSource = new WeakReference<>(null);
                return refreshCache();
            }
            return cached;
        }
        return null;
    }

    private @Nullable DigiVisSource refreshCache() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Level level = server != null ? server.getLevel(world) : null;
        if (level == null) {
            return null;
        }
        var tile = level.getBlockEntity(pos);
        if (tile == null) {
            return null;
        }
        DigiVisSource source;
        if (side == null) {
            if (!(tile instanceof DigiVisSource blockSource)) {
                return null;
            }
            source = blockSource;
        } else {
            if (!(tile instanceof IPartHost host)) {
                return null;
            }
            if (!(host.getPart(side) instanceof DigiVisSource partSource)) {
                return null;
            }
            source = partSource;
        }
        if (source.uid() != uid) {
            return null;
        }
        visSource = new WeakReference<>(source);
        return source;
    }
}
