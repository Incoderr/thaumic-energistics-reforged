package thaumicenergistics.client.knowledge;

import appeng.client.gui.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.knowledge.KnowledgeInscriberMenu;

public final class KnowledgeInscriberScreen extends AbstractContainerScreen<KnowledgeInscriberMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/knowledge.inscriber.png");
    private static final ResourceLocation BACKGROUND = ThaumicEnergistics.id("textures/research/research.background.png");
    private static final int GUI_WIDTH = 210;
    private static final int GUI_HEIGHT = 244;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 6;

    private SaveButton saveButton;

    public KnowledgeInscriberScreen(KnowledgeInscriberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        saveButton = addRenderableWidget(new SaveButton(leftPos + 141, topPos + 109));
        addRenderableWidget(new ClearButton(leftPos + 80, topPos + 89));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        saveButton.update(menu.saveState());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        long slowTime = System.currentTimeMillis() / 100;
        float red = Math.abs(1.0F - slowTime % 50 / 50.0F * 2.0F);
        graphics.setColor(red, 1.0F - red, 1.0F, 1.0F);
        graphics.blit(BACKGROUND, leftPos + 80, topPos + 106, 55 - (int) (slowTime % 10), 25, 35, 20);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
    }

    private void press(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private final class SaveButton extends AbstractButton {
        private KnowledgeInscriberMenu.SaveState state = KnowledgeInscriberMenu.SaveState.MISSING_CORE;

        SaveButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
            update(state);
        }

        void update(KnowledgeInscriberMenu.SaveState newState) {
            state = newState;
            active = state == KnowledgeInscriberMenu.SaveState.SAVE || state == KnowledgeInscriberMenu.SaveState.DELETE;
            String key = switch (state) {
                case INVALID_RECIPE -> "invalid";
                case CORE_FULL -> "full";
                case MISSING_CORE -> "missing";
                case SAVE -> "save";
                case DELETE -> "delete";
            };
            setTooltip(Tooltip.create(Component.translatable("thaumicenergistics.tooltip.button.inscriber." + key)));
        }

        @Override
        public void onPress() {
            press(KnowledgeInscriberMenu.BUTTON_SAVE_DELETE);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(graphics);
            if (state == KnowledgeInscriberMenu.SaveState.SAVE) {
                Icon.WHITE_ARROW_DOWN.getBlitter().dest(getX(), getY()).blit(graphics);
            } else if (state == KnowledgeInscriberMenu.SaveState.DELETE) {
                Icon.CLEAR.getBlitter().dest(getX(), getY()).blit(graphics);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class ClearButton extends AbstractButton {
        ClearButton(int x, int y) {
            super(x, y, 8, 8, Component.empty());
        }

        @Override
        public void onPress() {
            press(KnowledgeInscriberMenu.BUTTON_CLEAR);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Icon.S_CLEAR.getBlitter().dest(getX(), getY(), width, height).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
