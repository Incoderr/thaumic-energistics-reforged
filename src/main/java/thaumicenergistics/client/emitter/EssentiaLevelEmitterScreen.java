package thaumicenergistics.client.emitter;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.essentia.AspectWidgets;
import thaumicenergistics.emitter.EssentiaLevelEmitterMenu;

public final class EssentiaLevelEmitterScreen extends AbstractContainerScreen<EssentiaLevelEmitterMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.level.emitter.png");
    private static final ResourceLocation WIDGET_TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.io.bus.png");
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
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        amountField = new EditBox(font, leftPos + AMOUNT_X, topPos + AMOUNT_Y, AMOUNT_WIDTH, AMOUNT_HEIGHT, Component.empty());
        amountField.setBordered(false);
        amountField.setMaxLength(AMOUNT_MAX_CHARS);
        amountField.setTextColor(0xFFFFFF);
        amountField.setFilter(text -> text.chars().allMatch(Character::isDigit));
        amountField.setValue(Long.toString(menu.threshold));
        shownThreshold = menu.threshold;
        amountField.setResponder(text -> {
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
        return ADJUSTMENTS[index] * (Screen.hasShiftDown() ? 100 : 1);
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean shifted = Screen.hasShiftDown();
        for (int index = 0; index < adjustButtons.length; index++) {
            adjustButtons[index].setMessage(Component.literal(label(index, shifted)));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        Aspect aspect = menu.tracked();
        if (aspect != null && isOverWidget(mouseX, mouseY) && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, AspectWidgets.tooltip(aspect), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
        graphics.blit(WIDGET_TEXTURE, WIDGET_X, WIDGET_Y, 79, 39, WIDGET_SIZE, WIDGET_SIZE);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverWidget(mouseX, mouseY)) {
            menu.clickAspect(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (amountField.isFocused()) {
            amountField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
