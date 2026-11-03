package thaumicenergistics.cells;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.essentia.EssentiaKey;
import thaumicenergistics.essentia.EssentiaKeyType;

public final class EssentiaCellInventory implements StorageCell {
    public static final long CREATIVE_AMOUNT = 2000000000L;

    private final ItemStack stack;
    private final @Nullable ISaveProvider saveProvider;
    private final EssentiaStorageType type;
    private final Map<Aspect, Long> stored = new LinkedHashMap<>();
    private final List<Aspect> partition = new ArrayList<>();
    private final long totalBytes;
    private final long totalEssentia;
    private long usedEssentia;
    private boolean persisted = true;

    public EssentiaCellInventory(ItemStack stack, @Nullable ISaveProvider saveProvider, EssentiaStorageType type) {
        this.stack = stack;
        this.saveProvider = saveProvider;
        this.type = type;
        this.totalBytes = type.capacity();
        this.totalEssentia = totalBytes * EssentiaKeyType.ESSENTIA_PER_BYTE;
        EssentiaCellData data = CellsFeature.CELL_DATA.getOrDefault(stack, EssentiaCellData.EMPTY);
        for (EssentiaCellData.Entry entry : data.stored()) {
            if (entry.amount() > 0 && stored.size() < type.maxTypes()) {
                stored.merge(entry.aspect(), entry.amount(), Long::sum);
                usedEssentia += entry.amount();
            }
        }
        for (Aspect aspect : data.partition()) {
            if (!partition.contains(aspect)) {
                partition.add(aspect);
            }
        }
    }

    public static @Nullable EssentiaCellInventory of(ItemStack stack, @Nullable ISaveProvider saveProvider) {
        return stack.getItem() instanceof EssentiaCellItem item ? new EssentiaCellInventory(stack, saveProvider, item.storageType()) : null;
    }

    public EssentiaStorageType storageType() {
        return type;
    }

    public boolean isCreative() {
        return type.isCreative();
    }

    public long getTotalBytes() {
        return isCreative() ? 0 : totalBytes;
    }

    public long getUsedBytes() {
        return isCreative() ? 0 : (long) Math.ceil(usedEssentia / (double) EssentiaKeyType.ESSENTIA_PER_BYTE);
    }

    public long getFreeBytes() {
        return getTotalBytes() - getUsedBytes();
    }

    public int getTotalTypes() {
        return type.maxTypes();
    }

    public int getUsedTypes() {
        return isCreative() ? partition.size() : stored.size();
    }

    public boolean isPartitioned() {
        return isCreative() || !partition.isEmpty();
    }

    public List<Aspect> getPartition() {
        return List.copyOf(partition);
    }

    public Map<Aspect, Long> getStoredEssentia() {
        if (isCreative()) {
            Map<Aspect, Long> result = new LinkedHashMap<>();
            for (Aspect aspect : partition) {
                result.put(aspect, 1L);
            }
            return result;
        }
        return new LinkedHashMap<>(stored);
    }

    public boolean addToPartition(Aspect aspect) {
        if (partition.contains(aspect)) {
            return false;
        }
        partition.add(aspect);
        changed();
        return true;
    }

    public boolean removeFromPartition(Aspect aspect) {
        if (!partition.remove(aspect)) {
            return false;
        }
        changed();
        return true;
    }

    public boolean replaceInPartition(Aspect original, Aspect replacement) {
        int index = partition.indexOf(original);
        if (index < 0) {
            return false;
        }
        partition.set(index, replacement);
        changed();
        return true;
    }

    public void clearPartition() {
        partition.clear();
        changed();
    }

    public void partitionToContents() {
        if (isCreative()) {
            return;
        }
        partition.clear();
        partition.addAll(stored.keySet());
        changed();
    }

    private boolean canAccept(Aspect aspect) {
        if (isCreative()) {
            return false;
        }
        if (!partition.isEmpty() && !partition.contains(aspect)) {
            return false;
        }
        return stored.containsKey(aspect) || stored.size() < type.maxTypes();
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null || amount <= 0 || !canAccept(aspect)) {
            return 0;
        }
        long toStore = Math.min(amount, totalEssentia - usedEssentia);
        if (toStore <= 0) {
            return 0;
        }
        if (mode == Actionable.MODULATE) {
            stored.merge(aspect, toStore, Long::sum);
            usedEssentia += toStore;
            changed();
        }
        return toStore;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null || amount <= 0) {
            return 0;
        }
        if (isCreative()) {
            return partition.contains(aspect) ? amount : 0;
        }
        Long current = stored.get(aspect);
        if (current == null) {
            return 0;
        }
        long extracted = Math.min(current, amount);
        if (mode == Actionable.MODULATE) {
            if (extracted == current) {
                stored.remove(aspect);
            } else {
                stored.put(aspect, current - extracted);
            }
            usedEssentia -= extracted;
            changed();
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (isCreative()) {
            for (Aspect aspect : partition) {
                out.add(EssentiaKey.of(aspect), CREATIVE_AMOUNT);
            }
            return;
        }
        for (Map.Entry<Aspect, Long> entry : stored.entrySet()) {
            out.add(EssentiaKey.of(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        Aspect aspect = EssentiaKey.aspectOf(what);
        if (aspect == null || isCreative()) {
            return false;
        }
        return partition.isEmpty() ? stored.containsKey(aspect) : partition.contains(aspect);
    }

    @Override
    public CellState getStatus() {
        if (isCreative()) {
            return CellState.TYPES_FULL;
        }
        if (getUsedBytes() == getTotalBytes()) {
            return CellState.FULL;
        }
        if (getUsedTypes() == getTotalTypes()) {
            return CellState.TYPES_FULL;
        }
        return stored.isEmpty() ? CellState.EMPTY : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return type.idleDrain();
    }

    @Override
    public void persist() {
        if (persisted) {
            return;
        }
        List<EssentiaCellData.Entry> entries = new ArrayList<>();
        for (Map.Entry<Aspect, Long> entry : stored.entrySet()) {
            entries.add(new EssentiaCellData.Entry(entry.getKey(), entry.getValue()));
        }
        if (entries.isEmpty() && partition.isEmpty()) {
            CellsFeature.CELL_DATA.remove(stack);
        } else {
            CellsFeature.CELL_DATA.set(stack, new EssentiaCellData(entries, partition));
        }
        persisted = true;
    }

    @Override
    public Component getDescription() {
        return stack.getHoverName();
    }

    private void changed() {
        persisted = false;
        if (saveProvider != null) {
            saveProvider.saveChanges();
        } else {
            persist();
        }
    }
}
