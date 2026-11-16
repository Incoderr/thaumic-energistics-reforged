package thaumicenergistics.client.emitter;

import thaumicenergistics.client.platform.TeGuiBlit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.essentia.AspectWidgets;
import thaumicenergistics.emitter.EssentiaLevelEmitterMenu;

public final class EssentiaLevelEmitterScreen extends AbstractContainerScreen<EssentiaLevelEmitterMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.level.emitter.png");
    private static final Identifier WIDGET_TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.io.bus.png");
    private static final long[] ADJUSTMENTS = {1, 10, 100, -1, -10, -100};
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 184;
    private static final int TITLE_X = 8;
    private static final int TITLE_Y = 5;
    private static final int AMOUNT_X = 25;
    private static final int AMOUNT_Y = 44;
    private static final int AMOUNT_WIDTH = 80;
    private static final int AMOUNT_HEIGHT = 10;
    private static final int AMOUNT_MAX_CHARS = 10;
    private static final int BUTTON_X = 15;
    private static final int BUTTON_Y = 17;
    private static final int BUTTON_WIDTH = 42;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_PADDING_HORZ = 8;
    private static final int BUTTON_PADDING_VERT = 22;
    private static final int WIDGET_X = 123;
    private static final int WIDGET_Y = 39;
    private static final int WIDGET_SIZE = 18;
    private static final int HOVER_COLOR = 0x80FFFFFF;

    private final Button[] adjustButtons = new Button[ADJUSTMENTS.length];
    private EditBox amountField;
    private ServerSettingToggleButton<RedstoneMode> redstoneButton;
    private long shownThreshold = -1;

    public EssentiaLevelEmitterScreen(EssentiaLevelEmitterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        amountField = new EditBox(font, leftPos + AMOUNT_X, topPos + AMOUNT_Y, AMOUNT_WIDTH, AMOUNT_HEIGHT, Component.empty());
        amountField.setBordered(false);
        amountField.setMaxLength(AMOUNT_MAX_CHARS);
        amountField.setTextColor(0xFFFFFF);
        amountField.setValue(Long.toString(menu.threshold));
        shownThreshold = menu.threshold;
        amountField.setResponder(text -> {
            if (!text.chars().allMatch(Character::isDigit)) {
                amountField.setValue(text.replaceAll("[^0-9]", ""));
                return;
            }
            long value = 0;
            try {
                value = Long.parseLong(text);
            } catch (NumberFormatException ignored) {
            }
            shownThreshold = value;
            menu.setThreshold(value);
        });
        addRenderableWidget(amountField);
        setInitialFocus(amountField);
        for (int index = 0; index < ADJUSTMENTS.length; index++) {
            int row = index / 3;
            int column = index % 3;
            int button = index;
            adjustButtons[index] = addRenderableWidget(Button.builder(Component.literal(label(index, false)), pressed -> menu.adjustThreshold(adjustment(button)))
                .bounds(leftPos + BUTTON_X + column * (BUTTON_WIDTH + BUTTON_PADDING_HORZ), topPos + BUTTON_Y + row * (BUTTON_HEIGHT + BUTTON_PADDING_VERT), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }
        redstoneButton = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, menu.redstoneMode);
        redstoneButton.setX(leftPos - 18);
        redstoneButton.setY(topPos + 2);
        addRenderableWidget(redstoneButton);
    }

    private static long adjustment(int index) {
        return ADJUSTMENTS[index] * (Minecraft.getInstance().hasShiftDown() ? 100 : 1);
    }

    private static String label(int index, boolean shifted) {
        long value = ADJUSTMENTS[index] * (shifted ? 100 : 1);
        return value > 0 ? "+" + value : Long.toString(value);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        redstoneButton.set(menu.redstoneMode);
        if (menu.threshold != shownThreshold) {
            shownThreshold = menu.threshold;
            amountField.setValue(Long.toString(menu.threshold));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean shifted = Minecraft.getInstance().hasShiftDown();
        for (int index = 0; index < adjustButtons.length; index++) {
            adjustButtons[index].setMessage(Component.literal(label(index, shifted)));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Aspect aspect = menu.tracked();
        if (aspect != null && isOverWidget(mouseX, mouseY) && menu.getCarried().isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, AspectWidgets.tooltip(aspect), mouseX, mouseY);
        } else {
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, TITLE_X, TITLE_Y, 0, false);
        TeGuiBlit.blit(graphics, WIDGET_TEXTURE, WIDGET_X, WIDGET_Y, (float) (79), (float) (39), WIDGET_SIZE, WIDGET_SIZE, 256, 256);
        if (isOverWidget(mouseX, mouseY)) {
            graphics.fill(WIDGET_X + 1, WIDGET_Y + 1, WIDGET_X + WIDGET_SIZE - 1, WIDGET_Y + WIDGET_SIZE - 1, HOVER_COLOR);
        }
        Aspect aspect = menu.tracked();
        if (aspect != null) {
            AspectWidgets.draw(graphics, WIDGET_X + 1, WIDGET_Y + 1, aspect);
        }
    }

    private boolean isOverWidget(double mouseX, double mouseY) {
        double x = mouseX - leftPos - WIDGET_X;
        double y = mouseY - topPos - WIDGET_Y;
        return x >= 0 && y >= 0 && x < WIDGET_SIZE && y < WIDGET_SIZE;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (isOverWidget(mouseX, mouseY)) {
            menu.clickAspect(true);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (amountField.isFocused()) {
            amountField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
