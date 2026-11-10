package thaumicenergistics;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CRUCIBLE_ASSEMBLER_ENABLED;
    public static final ModConfigSpec.IntValue CRUCIBLE_ASSEMBLER_TICKS;
    public static final ModConfigSpec.DoubleValue CRUCIBLE_ASSEMBLER_POWER;
    public static final ModConfigSpec.BooleanValue INFUSION_ASSEMBLER_ENABLED;
    public static final ModConfigSpec.IntValue INFUSION_ASSEMBLER_TICKS;
    public static final ModConfigSpec.DoubleValue INFUSION_ASSEMBLER_POWER;

    static {
        BUILDER.translation("thaumicenergistics.configuration.crucibleAssembler").push("crucibleAssembler");
        CRUCIBLE_ASSEMBLER_ENABLED = BUILDER.translation("thaumicenergistics.configuration.enabled").define("enabled", false);
        CRUCIBLE_ASSEMBLER_TICKS = BUILDER.translation("thaumicenergistics.configuration.ticksPerCraft").defineInRange("ticksPerCraft", 20, 1, 1200);
        CRUCIBLE_ASSEMBLER_POWER = BUILDER.translation("thaumicenergistics.configuration.powerPerTick").defineInRange("powerPerTick", 1.5D, 0.0D, 10000.0D);
        BUILDER.pop();
        BUILDER.translation("thaumicenergistics.configuration.infusionAssembler").push("infusionAssembler");
        INFUSION_ASSEMBLER_ENABLED = BUILDER.translation("thaumicenergistics.configuration.enabled").define("enabled", false);
        INFUSION_ASSEMBLER_TICKS = BUILDER.translation("thaumicenergistics.configuration.ticksPerCraft").defineInRange("ticksPerCraft", 20, 1, 1200);
        INFUSION_ASSEMBLER_POWER = BUILDER.translation("thaumicenergistics.configuration.powerPerTick").defineInRange("powerPerTick", 1.5D, 0.0D, 10000.0D);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static boolean flag(ModConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.getAsBoolean() : value.getDefault();
    }

    public static int number(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.getAsInt() : value.getDefault();
    }

    public static double amount(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.getAsDouble() : value.getDefault();
    }

    public static boolean crucibleAssembler() {
        return flag(CRUCIBLE_ASSEMBLER_ENABLED);
    }

    public static boolean infusionAssembler() {
        return flag(INFUSION_ASSEMBLER_ENABLED);
    }

    public static int ticksPerCraft(ModConfigSpec.IntValue base, int speedCards, int maxSpeedCards) {
        int ticks = number(base);
        return Math.max(1, ticks - ticks * speedCards / (maxSpeedCards + 1));
    }
}
