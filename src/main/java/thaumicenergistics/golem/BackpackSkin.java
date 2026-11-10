package thaumicenergistics.golem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import javax.annotation.Nullable;
import thaumicenergistics.ThaumicEnergistics;

public enum BackpackSkin {
    THAUMIUM("thaum"),
    STONE("stone"),
    STRAW("straw"),
    GREATWOOD("wood"),
    FLESH("flesh"),
    CLAY("clay"),
    IRON("iron"),
    TALLOW("tallow"),
    GOLD("gold"),
    DIAMOND("diamond");

    private static final BackpackSkin[] VALUES = values();
    private final ResourceLocation texture;

    BackpackSkin(String texture) {
        this.texture = ThaumicEnergistics.id("textures/models/golembackpack/" + texture + ".png");
    }

    public ResourceLocation texture() {
        return texture;
    }

    public static BackpackSkin byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : THAUMIUM;
    }

    public static @Nullable BackpackSkin fromBlock(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if ("thaumcraft".equals(id.getNamespace())) {
            return switch (id.getPath()) {
                case "thaumium_block" -> THAUMIUM;
                case "tallow_block" -> TALLOW;
                case "greatwood_log" -> GREATWOOD;
                case "flesh_block" -> FLESH;
                default -> null;
            };
        }
        if (block == Blocks.STONE) {
            return STONE;
        }
        if (block == Blocks.HAY_BLOCK) {
            return STRAW;
        }
        if (block == Blocks.BRICKS) {
            return CLAY;
        }
        if (block == Blocks.IRON_BLOCK) {
            return IRON;
        }
        if (block == Blocks.GOLD_BLOCK) {
            return GOLD;
        }
        if (block == Blocks.DIAMOND_BLOCK) {
            return DIAMOND;
        }
        return null;
    }
}
