package thaumicenergistics.client.terminal;

import appeng.api.config.ViewItems;
import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import appeng.core.localization.ButtonToolTips;
import appeng.util.ReadableNumberConverter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.essentia.AspectWidgets;
import thaumicenergistics.terminal.EssentiaListPayload;
import thaumicenergistics.terminal.EssentiaSortMode;
import thaumicenergistics.terminal.EssentiaTerminalMenu;

public final class EssentiaTerminalScreen extends AbstractContainerScreen<EssentiaTerminalMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/essentia.terminal.png");
    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller");
    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
    private static final int GUI_WIDTH = 195;
    private static final int GUI_HEIGHT = 204;
    private static final int WIDGET_X = 7;
    private static final int WIDGET_Y = 17;
    private static final int WIDGET_SIZE = 18;
    private static final int WIDGETS_PER_ROW = 9;
    private static final int WIDGET_ROWS = 4;
    private static final int WIDGETS_PER_PAGE = WIDGETS_PER_ROW * WIDGET_ROWS;
    private static final int SCROLLBAR_X = 175;
    private static final int SCROLLBAR_Y = 18;
    private static final int SCROLLBAR_HEIGHT = 70;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int SELECTED_INFO_X = 45;
    private static final int SELECTED_NAME_Y = 91;
    private static final int SELECTED_AMOUNT_Y = 101;
    private static final int SELECTOR_BORDER_COLOR = 0xFF00FFFF;
    private static final int GRADIENT_COUNT = 15;

    private EditBox searchBar;
    private String searchTerm = "";
    private List<EssentiaListPayload.Entry> lastList = List.of();
    private EssentiaSortMode lastSort;
    private ViewItems lastView;
    private List<EssentiaListPayload.Entry> matching = new ArrayList<>();
    private int scroll;
    private boolean draggingScroller;

    public EssentiaTerminalScreen(EssentiaTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        searchBar = new EditBox(font, leftPos + 98, topPos + 6, 69, 10, Component.empty());
        searchBar.setBordered(false);
        searchBar.setMaxLength(14);
        searchBar.setTextColor(0xFFFFFF);
        searchBar.setValue(searchTerm);
        searchBar.setResponder(value -> {
            searchTerm = value.trim().toLowerCase();
            updateView();
        });
        addRenderableWidget(searchBar);
        addRenderableWidget(new ModeButton(leftPos - 18, topPos + 9,
            () -> menu.sortMode == EssentiaSortMode.AMOUNT ? Icon.SORT_BY_AMOUNT : Icon.SORT_BY_NAME,
            () -> List.of(ButtonToolTips.SortBy.text(), (menu.sortMode == EssentiaSortMode.AMOUNT ? ButtonToolTips.NumberOfItems : ButtonToolTips.ItemName).text().withStyle(ChatFormatting.GRAY)),
            menu::cycleSortMode));
        addRenderableWidget(new ModeButton(leftPos - 18, topPos + 29,
            () -> switch (menu.viewMode) {
                case STORED -> Icon.VIEW_MODE_STORED;
                case CRAFTABLE -> Icon.VIEW_MODE_CRAFTING;
                default -> Icon.VIEW_MODE_ALL;
            },
            () -> List.of(ButtonToolTips.View.text(), (switch (menu.viewMode) {
                case STORED -> ButtonToolTips.StoredItems;
                case CRAFTABLE -> ButtonToolTips.Craftable;
                default -> ButtonToolTips.StoredCraftable;
            }).text().withStyle(ChatFormatting.GRAY)),
            menu::cycleViewMode));
        updateView();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.clientList() != lastList || menu.sortMode != lastSort || menu.viewMode != lastView) {
            updateView();
        }
    }

    private void updateView() {
        lastList = menu.clientList();
        lastSort = menu.sortMode;
        lastView = menu.viewMode;
        matching = new ArrayList<>();
        for (EssentiaListPayload.Entry entry : lastList) {
            if (lastView == ViewItems.CRAFTABLE && !entry.craftable() || lastView == ViewItems.STORED && entry.amount() == 0) {
                continue;
            }
            if (searchTerm.isEmpty() || entry.aspect().getName().contains(searchTerm) || entry.aspect().getTag().contains(searchTerm)) {
                matching.add(entry);
            }
        }
        Comparator<EssentiaListPayload.Entry> byName = Comparator.comparing(entry -> entry.aspect().getName());
        matching.sort(lastSort == EssentiaSortMode.AMOUNT
            ? Comparator.comparingLong(EssentiaListPayload.Entry::amount).reversed().thenComparing(byName)
            : byName);
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        int overflow = Math.max(0, matching.size() - WIDGETS_PER_PAGE);
        return (overflow + WIDGETS_PER_ROW - 1) / WIDGETS_PER_ROW;
    }

    private EssentiaListPayload.Entry entryAt(int widget) {
        int index = widget + scroll * WIDGETS_PER_ROW;
        return widget >= 0 && index < matching.size() ? matching.get(index) : null;
    }

    private int widgetAt(double mouseX, double mouseY) {
        int x = (int) Math.floor(mouseX) - leftPos - WIDGET_X;
        int y = (int) Math.floor(mouseY) - topPos - WIDGET_Y;
        if (x < 0 || y < 0 || x >= WIDGETS_PER_ROW * WIDGET_SIZE || y >= WIDGET_ROWS * WIDGET_SIZE) {
            return -1;
        }
        return x / WIDGET_SIZE + y / WIDGET_SIZE * WIDGETS_PER_ROW;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        EssentiaListPayload.Entry hovered = entryAt(widgetAt(mouseX, mouseY));
        if (hovered != null && menu.getCarried().isEmpty()) {
            graphics.renderComponentTooltip(font, AspectWidgets.tooltip(hovered.aspect()), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int max = maxScroll();
        int scrollerY = topPos + SCROLLBAR_Y + (max == 0 ? 0 : (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT) * scroll / max);
        graphics.blitSprite(max == 0 ? SCROLLER_DISABLED : SCROLLER, leftPos + SCROLLBAR_X, scrollerY, 12, SCROLLER_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 7, 6, 0, false);
        Aspect selected = menu.selected();
        for (int widget = 0; widget < WIDGETS_PER_PAGE; widget++) {
            EssentiaListPayload.Entry entry = entryAt(widget);
            if (entry == null) {
                break;
            }
            int x = WIDGET_X + widget % WIDGETS_PER_ROW * WIDGET_SIZE;
            int y = WIDGET_Y + widget / WIDGETS_PER_ROW * WIDGET_SIZE;
            if (entry.aspect() == selected) {
                drawSelector(graphics, x, y, entry.aspect().getColor());
            }
            AspectWidgets.draw(graphics, x + 1, y + 1, entry.aspect());
            drawAmount(graphics, x, y, entry);
        }
        if (selected != null) {
            for (EssentiaListPayload.Entry entry : menu.clientList()) {
                if (entry.aspect() == selected) {
                    graphics.drawString(font, Component.translatable("thaumicenergistics.gui.selected.aspect").append(": ").append(AspectWidgets.name(selected)), SELECTED_INFO_X, SELECTED_NAME_Y, 0, false);
                    graphics.drawString(font, Component.translatable("thaumicenergistics.gui.selected.amount").append(": " + entry.amount()), SELECTED_INFO_X, SELECTED_AMOUNT_Y, 0, false);
                    break;
                }
            }
        }
    }

    private void drawSelector(GuiGraphics graphics, int x, int y, int aspectColor) {
        int time = (int) (System.currentTimeMillis() / 45L);
        int index = Math.abs(Math.abs(time % (GRADIENT_COUNT * 2)) - GRADIENT_COUNT);
        float delta = index / (float) GRADIENT_COUNT;
        int alpha = Math.round(Mth.lerp(delta, 0x70, 0x20));
        graphics.fill(x, y, x + WIDGET_SIZE, y + WIDGET_SIZE, FastColor.ARGB32.color(alpha, aspectColor));
        graphics.renderOutline(x, y, WIDGET_SIZE, WIDGET_SIZE, SELECTOR_BORDER_COLOR);
    }

    private void drawAmount(GuiGraphics graphics, int x, int y, EssentiaListPayload.Entry entry) {
        boolean hideAmount = menu.viewMode == ViewItems.CRAFTABLE;
        String text;
        if (entry.amount() > 0 && !hideAmount) {
            text = ReadableNumberConverter.format(entry.amount(), 4);
        } else if (entry.craftable()) {
            text = Component.translatable("thaumicenergistics.gui.craft").getString();
        } else {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        float right = (x + WIDGET_SIZE - 1) * 2;
        float bottom = (y + WIDGET_SIZE - 2) * 2;
        graphics.drawString(font, text, (int) (right - font.width(text)), (int) (bottom - font.lineHeight + 1), 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int widget = widgetAt(mouseX, mouseY);
        if (widget >= 0) {
            EssentiaListPayload.Entry entry = entryAt(widget);
            if (entry == null) {
                return true;
            }
            if (!menu.getCarried().isEmpty()) {
                menu.interactWithHeldItem(entry.aspect().getTag());
                return true;
            }
            boolean left = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
            boolean right = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            boolean middle = button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            if (!right && entry.craftable() && (menu.viewMode == ViewItems.CRAFTABLE || middle || left && entry.amount() == 0)) {
                menu.requestCraft(entry.aspect().getTag());
            }
            Aspect selected = menu.selected();
            menu.selectAspect(entry.aspect() == selected ? "" : entry.aspect().getTag());
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (isOverSearch(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                searchBar.setValue("");
                return true;
            }
        } else {
            searchBar.setFocused(false);
        }
        if (isOverScrollbar(mouseX, mouseY) && maxScroll() > 0) {
            draggingScroller = true;
            scrollTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroller) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroller = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX <= leftPos + GUI_WIDTH && !Screen.hasShiftDown() && scrollY != 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (searchBar.isFocused()) {
            searchBar.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isOverSearch(double mouseX, double mouseY) {
        return mouseX >= searchBar.getX() && mouseX < searchBar.getX() + searchBar.getWidth() && mouseY >= searchBar.getY() && mouseY < searchBar.getY() + searchBar.getHeight();
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        double x = mouseX - leftPos - SCROLLBAR_X;
        double y = mouseY - topPos - SCROLLBAR_Y;
        return x >= 0 && x < 12 && y >= 0 && y < SCROLLBAR_HEIGHT;
    }

    private void scrollTo(double mouseY) {
        double relative = (mouseY - topPos - SCROLLBAR_Y - SCROLLER_HEIGHT / 2.0) / (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT);
        scroll = Mth.clamp((int) Math.round(relative * maxScroll()), 0, maxScroll());
    }

    private static final class ModeButton extends IconButton {
        private final Supplier<Icon> icon;
        private final Supplier<List<Component>> message;
        private final Consumer<Boolean> action;

        ModeButton(int x, int y, Supplier<Icon> icon, Supplier<List<Component>> message, Consumer<Boolean> action) {
            super(button -> {
            });
            this.icon = icon;
            this.message = message;
            this.action = action;
            setX(x);
            setY(y);
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (active && visible && isValidClickButton(button) && clicked(mouseX, mouseY)) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                action.accept(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                return true;
            }
            return false;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            List<Component> lines = message.get();
            setTooltip(Tooltip.create(lines.get(0).copy().append("\n").append(lines.get(1))));
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected Icon getIcon() {
            return icon.get();
        }

        @Override
        public List<Component> getTooltipMessage() {
            return message.get();
        }
    }
}
