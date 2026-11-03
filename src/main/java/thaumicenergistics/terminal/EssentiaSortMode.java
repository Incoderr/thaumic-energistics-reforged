package thaumicenergistics.terminal;

public enum EssentiaSortMode {
    ALPHABETIC,
    AMOUNT;

    public EssentiaSortMode next(boolean backwards) {
        EssentiaSortMode[] values = values();
        return values[(ordinal() + (backwards ? values.length - 1 : 1)) % values.length];
    }
}
