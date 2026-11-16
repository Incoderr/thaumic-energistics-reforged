package thaumicenergistics.client.workbench;

import thaumicenergistics.client.platform.TeGuiBlit;
import net.minecraft.client.input.MouseButtonEvent;
import appeng.client.gui.style.Blitter;
import appeng.util.Icon;
import appeng.client.gui.widgets.IconButton;
import appeng.core.localization.ButtonToolTips;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.essentia.AspectWidgets;
import thaumicenergistics.client.platform.TeClientNetwork;
import thaumicenergistics.workbench.EssentiaCellWorkbenchMenu;
import thaumicenergistics.workbench.WorkbenchActionPayload;

public final class EssentiaCellWorkbenchScreen extends AbstractContainerScreen<EssentiaCellWorkbenchMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.cell.workbench.png");
    private static final Identifier SLOT_TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.io.bus.png");
    private static final int WIDGET_X = 7;
    private static final int WIDGET_Y = 28;
    private static final int WIDGETS_PER_ROW = 9;
    private static final int WIDGET_COUNT = 63;
    private static final int WIDGET_SIZE = 18;

    public EssentiaCellWorkbenchScreen(EssentiaCellWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 251);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new WorkbenchButton(Icon.COG, leftPos - 18, topPos + 28, WorkbenchActionPayload.PARTITION_TO_CONTENTS,
            ButtonToolTips.PartitionStorage.text(), ButtonToolTips.PartitionStorageHint.text()));
        addRenderableWidget(new WorkbenchButton(Icon.CLEAR, leftPos - 18, topPos + 8, WorkbenchActionPayload.CLEAR,
            ButtonToolTips.Clear.text(), ButtonToolTips.ClearSettings.text()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int widget = widgetAt(mouseX, mouseY);
        Aspect aspect = widget < 0 ? null : aspectAt(widget);
        if (aspect != null && menu.getCarried().isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, AspectWidgets.tooltip(aspect), mouseX, mouseY);
        } else {
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), imageWidth, imageHeight, 256, 256);
        Blitter.icon(Icon.BACKGROUND_STORAGE_CELL).dest(leftPos + EssentiaCellWorkbenchMenu.CELL_SLOT_X, topPos + EssentiaCellWorkbenchMenu.CELL_SLOT_Y).blit(graphics);
        for (int index = 0; index < WIDGET_COUNT; index++) {
            int x = leftPos + widgetX(index);
            int y = topPos + widgetY(index);
            TeGuiBlit.blit(graphics, SLOT_TEXTURE, x, y, (float) (79), (float) (39), WIDGET_SIZE, WIDGET_SIZE, 256, 256);
            Aspect aspect = aspectAt(index);
            if (aspect == null) {
                continue;
            }
            AspectWidgets.draw(graphics, x + 1, y + 1, aspect);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 6, 6, 0, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        int widget = widgetAt(mouseX, mouseY);
        if (widget >= 0) {
            Aspect aspect = aspectAt(widget);
            if (aspect != null || !menu.getCarried().isEmpty()) {
                TeClientNetwork.sendToServer(new WorkbenchActionPayload(WorkbenchActionPayload.CLICK_SLOT, Optional.ofNullable(aspect)));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Aspect aspectAt(int index) {
        List<Aspect> partition = menu.clientPartition();
        return index < partition.size() ? partition.get(index) : null;
    }

    private int widgetAt(double mouseX, double mouseY) {
        for (int index = 0; index < WIDGET_COUNT; index++) {
            int x = leftPos + widgetX(index);
            int y = topPos + widgetY(index);
            if (mouseX >= x && mouseX < x + WIDGET_SIZE && mouseY >= y && mouseY < y + WIDGET_SIZE) {
                return index;
            }
        }
        return -1;
    }

    private static int widgetX(int index) {
        return WIDGET_X + index % WIDGETS_PER_ROW * WIDGET_SIZE;
    }

    private static int widgetY(int index) {
        return WIDGET_Y + index / WIDGETS_PER_ROW * WIDGET_SIZE;
    }

    private static final class WorkbenchButton extends IconButton {
        private final Icon icon;
        private final List<Component> message;

        WorkbenchButton(Icon icon, int x, int y, int action, Component title, Component hint) {
            super(button -> TeClientNetwork.sendToServer(new WorkbenchActionPayload(action, Optional.empty())));
            this.icon = icon;
            this.message = List.of(title, hint);
            setX(x);
            setY(y);
            setTooltip(Tooltip.create(title.copy().append("\n").append(hint.copy().withStyle(ChatFormatting.GRAY))));
        }

        @Override
        protected Icon getIcon() {
            return icon;
        }

        @Override
        public List<Component> getTooltipMessage() {
            return message;
        }
    }
}
