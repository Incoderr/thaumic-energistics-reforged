package thaumicenergistics.cells;

import net.minecraft.world.item.Rarity;

public enum EssentiaStorageType {
    TYPE_1K("1k", 1024, 12, Rarity.COMMON, 0.5),
    TYPE_4K("4k", 4096, 12, Rarity.UNCOMMON, 1.0),
    TYPE_16K("16k", 16348, 12, Rarity.UNCOMMON, 1.5),
    TYPE_64K("64k", 65536, 12, Rarity.RARE, 2.0),
    CREATIVE("creative", 0, 63, Rarity.EPIC, 0.0);

    private final String suffix;
    private final long capacity;
    private final int maxTypes;
    private final Rarity rarity;
    private final double idleDrain;

    EssentiaStorageType(String suffix, long capacity, int maxTypes, Rarity rarity, double idleDrain) {
        this.suffix = suffix;
        this.capacity = capacity;
        this.maxTypes = maxTypes;
        this.rarity = rarity;
        this.idleDrain = idleDrain;
    }

    public String suffix() {
        return suffix;
    }

    public long capacity() {
        return capacity;
    }

    public int maxTypes() {
        return maxTypes;
    }

    public Rarity rarity() {
        return rarity;
    }

    public double idleDrain() {
        return idleDrain;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }
}
