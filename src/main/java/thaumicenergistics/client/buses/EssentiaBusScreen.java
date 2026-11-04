package thaumicenergistics.client.buses;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.buses.EssentiaBusHost;
import thaumicenergistics.buses.EssentiaBusMenu;
import thaumicenergistics.client.essentia.AspectWidgets;

public final class EssentiaBusScreen extends AbstractContainerScreen<EssentiaBusMenu> {
    private static final ResourceLocation IO_TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.io.bus.png");
    private static final ResourceLocation STORAGE_TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.storage.bus.png");
    private static final int GUI_HEIGHT = 184;
    private static final int GUI_MAIN_WIDTH = 176;
    private static final int IO_WIDTH_NO_TOOL = 211;
    private static final int STORAGE_WIDTH_NO_TOOL = 210;
    private static final int WIDTH_WITH_TOOL = 246;
    private static final int IO_UPGRADES_WIDTH = 35;
    private static final int IO_UPGRADES_HEIGHT = 86;
    private static final int WIDGET_X = 61;
    private static final int WIDGET_Y = 21;
    private static final int WIDGET_SIZE = 18;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 5;
    private static final int HOVER_COLOR = 0x80FFFFFF;

    private final boolean storageBus;
    private ServerSettingToggleButton<RedstoneMode> redstoneButton;
    private VoidButton voidButton;

    public EssentiaBusScreen(EssentiaBusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        storageBus = menu.getHost().isStorageBus();
        imageWidth = menu.hasToolbox() ? WIDTH_WITH_TOOL : storageBus ? STORAGE_WIDTH_NO_TOOL : IO_WIDTH_NO_TOOL;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        EssentiaBusHost host = menu.getHost();
        if (storageBus) {
            TabButton priority = new TabButton(Icon.PRIORITY, GuiText.Priority.text(), button -> menu.openPriority(true));
            priority.setX(leftPos + 154);
            priority.setY(topPos);
            addRenderableWidget(priority);
        } else {
            redstoneButton = new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, menu.redstoneMode);
            redstoneButton.setX(leftPos - 18);
            redstoneButton.setY(topPos + 2);
            addRenderableWidget(redstoneButton);
        }
        if (host.supportsVoid()) {
            voidButton = addRenderableWidget(new VoidButton(leftPos + (storageBus ? -18 : -19), topPos + (storageBus ? 8 : 2)));
        }
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        if (redstoneButton != null) {
            redstoneButton.visible = menu.redstoneControlled;
            redstoneButton.set(menu.redstoneMode);
        }
        if (voidButton != null) {
            if (!storageBus) {
                voidButton.setY(topPos + 2 + (menu.redstoneControlled ? 18 : 0));
            }
            voidButton.updateTooltip();
        }
    }

    private boolean isWidgetVisible(int index) {
        return menu.isFilterAvailable(index);
    }

    private int widgetAt(double mouseX, double mouseY) {
        int x = (int) Math.floor(mouseX) - leftPos - WIDGET_X;
        int y = (int) Math.floor(mouseY) - topPos - WIDGET_Y;
        if (x < 0 || y < 0 || x >= 3 * WIDGET_SIZE || y >= 3 * WIDGET_SIZE) {
            return -1;
        }
        return x / WIDGET_SIZE + y / WIDGET_SIZE * 3;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (storageBus) {
            graphics.blit(STORAGE_TEXTURE, leftPos, topPos, 0, 0, GUI_MAIN_WIDTH, GUI_HEIGHT);
            graphics.blit(STORAGE_TEXTURE, leftPos + 179, topPos, 179, 0, 32, 32);
            if (menu.hasToolbox()) {
                graphics.blit(STORAGE_TEXTURE, leftPos + 179, topPos + 93, 178, 93, 68, 68);
            }
        } else if (menu.hasToolbox()) {
            graphics.blit(IO_TEXTURE, leftPos, topPos, 0, 0, WIDTH_WITH_TOOL, GUI_HEIGHT);
        } else {
            graphics.blit(IO_TEXTURE, leftPos, topPos, 0, 0, GUI_MAIN_WIDTH, GUI_HEIGHT);
            graphics.blit(IO_TEXTURE, leftPos + GUI_MAIN_WIDTH, topPos, GUI_MAIN_WIDTH, 0, IO_UPGRADES_WIDTH, IO_UPGRADES_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
        int hovered = widgetAt(mouseX, mouseY);
        for (int index = 0; index < EssentiaBusHost.FILTER_SIZE; index++) {
            if (!isWidgetVisible(index)) {
                continue;
            }
            int x = WIDGET_X + index % 3 * WIDGET_SIZE;
            int y = WIDGET_Y + index / 3 * WIDGET_SIZE;
            graphics.blit(IO_TEXTURE, x, y, 79, 39, WIDGET_SIZE, WIDGET_SIZE);
            if (index == hovered) {
                graphics.fill(x + 1, y + 1, x + WIDGET_SIZE - 1, y + WIDGET_SIZE - 1, HOVER_COLOR);
            }
            Aspect aspect = menu.filter(index);
            if (aspect != null) {
                AspectWidgets.draw(graphics, x + 1, y + 1, aspect);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = widgetAt(mouseX, mouseY);
        Aspect aspect = hovered >= 0 && isWidgetVisible(hovered) ? menu.filter(hovered) : null;
        if (aspect != null && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, AspectWidgets.tooltip(aspect), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int widget = widgetAt(mouseX, mouseY);
        if (widget >= 0 && isWidgetVisible(widget)) {
            menu.clickFilter(widget);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private final class VoidButton extends AbstractButton {
        private static final ItemStack VOID_JAR = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:void_jar")));

        VoidButton(int x, int y) {
            super(x, y, 18, 18, Component.empty());
        }

        void updateTooltip() {
            setTooltip(Tooltip.create(Component.translatable("thaumicenergistics.tooltip.button.void")
                .append("\n")
                .append(Component.translatable(menu.voidAllowed ? "thaumicenergistics.tooltip.button.void.allowed" : "thaumicenergistics.tooltip.button.void.disabled").withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable("thaumicenergistics.tooltip.button.void.note").withStyle(ChatFormatting.ITALIC))));
        }

        @Override
        public void onPress() {
            menu.toggleVoid(true);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.renderItem(VOID_JAR, getX() + 1, getY() + 1);
            if (!menu.voidAllowed) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                Icon.INVALID.getBlitter().dest(getX() + 1, getY() + 1).blit(graphics);
                graphics.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
