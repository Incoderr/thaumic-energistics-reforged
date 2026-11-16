package thaumicenergistics.client.vibration;

import thaumicenergistics.client.platform.TeGuiBlit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.vibration.VibrationChamberBlockEntity;
import thaumicenergistics.vibration.VibrationChamberMenu;

public final class VibrationChamberScreen extends AbstractContainerScreen<VibrationChamberMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.vibration.chamber.png");
    private static final Identifier FURNACE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_alchemyfurnace.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 82;
    private static final int POWER_POS_X = 96;
    private static final int POWER_POS_Y = 32;
    private static final int POWER_TEX_U = 51;
    private static final int POWER_TEX_V = 83;
    private static final int POWER_WIDTH = 6;
    private static final int POWER_HEIGHT = 18;
    private static final int STORED_POS_X = 62;
    private static final int STORED_POS_Y = 16;
    private static final int STORED_TEX_X = 200;
    private static final int STORED_WIDTH = 8;
    private static final int STORED_HEIGHT = 48;
    private static final int VIAL_POS_X = 61;
    private static final int VIAL_POS_Y = 12;
    private static final int VIAL_TEX_X = 232;
    private static final int VIAL_WIDTH = 11;
    private static final int VIAL_HEIGHT = 56;
    private static final int FIRE_POS_X = 78;
    private static final int FIRE_POS_Y = 30;
    private static final int FIRE_TEX_X = 177;
    private static final int FIRE_WIDTH = 15;
    private static final int FIRE_HEIGHT = 20;
    private static final int TICK_AVG_COUNT = 4;

    private float ticksMovingAverage;
    private int averageTicksRemaining;

    public VibrationChamberScreen(VibrationChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int ticksRemaining = menu.ticksRemaining();
        if (ticksMovingAverage == 0) {
            ticksMovingAverage = ticksRemaining;
        } else {
            float invertedWeight = ticksRemaining > ticksMovingAverage ? 0.3F : 1.0F;
            ticksMovingAverage -= ticksMovingAverage / (TICK_AVG_COUNT * invertedWeight);
            ticksMovingAverage += ticksRemaining / (TICK_AVG_COUNT * invertedWeight);
        }
        averageTicksRemaining = (int) ticksMovingAverage;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<Component> tooltip = new ArrayList<>();
        if (isHovering(VIAL_POS_X, VIAL_POS_Y, VIAL_WIDTH, VIAL_HEIGHT, mouseX, mouseY)) {
            Aspect aspect = menu.storedAspect();
            int amount = menu.storedAmount();
            tooltip.add(amount > 0 && aspect != null ? Component.literal(String.format("%s x %d", aspect.getName(), amount)) : Component.translatable("thaumicenergistics.gui.vibration.chamber.empty"));
        } else if (isHovering(FIRE_POS_X, FIRE_POS_Y, FIRE_WIDTH, FIRE_HEIGHT, mouseX, mouseY)) {
            Aspect processing = menu.processingAspect();
            if (processing != null) {
                tooltip.add(Component.translatable("thaumicenergistics.gui.vibration.chamber.processing", processing.getName()));
                tooltip.add(Component.translatable("thaumicenergistics.gui.vibration.chamber.time_remaining", String.format("%.0f", averageTicksRemaining / 20.0F)));
            }
        } else if (isHovering(POWER_POS_X, POWER_POS_Y, POWER_WIDTH, POWER_HEIGHT, mouseX, mouseY)) {
            tooltip.add(Component.literal(String.format("%.0f AE/t", menu.powerPerTick())));
        }
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        } else {
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), imageWidth, imageHeight, 256, 256);
        float maxPower = menu.maxPowerPerTick();
        float powerPercent = 1.0F - (maxPower > 0 ? menu.powerPerTick() / maxPower : 0);
        int powerOffset = (int) (POWER_HEIGHT * powerPercent);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos + POWER_POS_X, topPos + POWER_POS_Y + powerOffset, (float) (POWER_TEX_U), (float) (POWER_TEX_V + powerOffset), POWER_WIDTH, POWER_HEIGHT - powerOffset, 256, 256);
        float storedPercent = 1.0F - menu.storedAmount() / (float) VibrationChamberBlockEntity.MAX_ESSENTIA_STORED;
        int storedOffset = (int) (STORED_HEIGHT * storedPercent);
        TeGuiBlit.blit(graphics, FURNACE, leftPos + STORED_POS_X, topPos + STORED_POS_Y + storedOffset, (float) (STORED_TEX_X), (float) (storedOffset), STORED_WIDTH, STORED_HEIGHT - storedOffset, 256, 256);
        TeGuiBlit.blit(graphics, FURNACE, leftPos + VIAL_POS_X, topPos + VIAL_POS_Y, (float) (VIAL_TEX_X), (float) (0), VIAL_WIDTH, VIAL_HEIGHT, 256, 256);
        int totalTicks = menu.totalTicks();
        float ticksPercent = 1.0F - (totalTicks > 0 ? menu.ticksRemaining() / (float) totalTicks : 0);
        int fireOffset = (int) (FIRE_HEIGHT * ticksPercent);
        TeGuiBlit.blit(graphics, FURNACE, leftPos + FIRE_POS_X, topPos + FIRE_POS_Y + fireOffset, (float) (FIRE_TEX_X), (float) (fireOffset), FIRE_WIDTH, FIRE_HEIGHT - fireOffset, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }
}
