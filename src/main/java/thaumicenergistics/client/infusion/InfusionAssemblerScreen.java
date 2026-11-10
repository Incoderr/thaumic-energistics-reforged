package thaumicenergistics.client.infusion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.infusion.InfusionAssemblerMenu;

public final class InfusionAssemblerScreen extends AbstractContainerScreen<InfusionAssemblerMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/infusion.assembler.png");
    private static final int MAIN_GUI_WIDTH = 176;
    private static final int UPGRADE_GUI_WIDTH = 234 - MAIN_GUI_WIDTH;
    private static final int GUI_HEIGHT = 197;
    private static final int UPGRADE_GUI_HEIGHT = 104;
    private static final int BAR_EMPTY_Y = 87;
    private static final int BAR_FULL_Y = 202;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_WIDTH = 4;
    private static final int PROGRESS_BAR_X = 149;

    public InfusionAssemblerScreen(InfusionAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = MAIN_GUI_WIDTH + UPGRADE_GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, MAIN_GUI_WIDTH, imageHeight);
        graphics.blit(TEXTURE, leftPos + MAIN_GUI_WIDTH, topPos, MAIN_GUI_WIDTH, 0, UPGRADE_GUI_WIDTH, UPGRADE_GUI_HEIGHT);
        int height = (int) Math.floor(menu.percentComplete() * BAR_HEIGHT);
        if (height > 0) {
            graphics.blit(TEXTURE, leftPos + PROGRESS_BAR_X, topPos + BAR_EMPTY_Y + BAR_HEIGHT - height, PROGRESS_BAR_X, BAR_FULL_Y + BAR_HEIGHT - height, BAR_WIDTH, height);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 6, 6, 0, false);
    }
}
