package thaumicenergistics.client.alchemy;

import thaumicenergistics.client.platform.TeGuiBlit;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.alchemy.CrucibleAssemblerMenu;

public final class CrucibleAssemblerScreen extends AbstractContainerScreen<CrucibleAssemblerMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/crucible.assembler.png");
    private static final int MAIN_GUI_WIDTH = 176;
    private static final int UPGRADE_GUI_WIDTH = 234 - MAIN_GUI_WIDTH;
    private static final int GUI_HEIGHT = 197;
    private static final int UPGRADE_GUI_HEIGHT = 104;
    private static final int BAR_EMPTY_Y = 87;
    private static final int BAR_FULL_Y = 202;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_WIDTH = 4;
    private static final int PROGRESS_BAR_X = 149;

    public CrucibleAssemblerScreen(CrucibleAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MAIN_GUI_WIDTH + UPGRADE_GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), MAIN_GUI_WIDTH, imageHeight, 256, 256);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos + MAIN_GUI_WIDTH, topPos, (float) (MAIN_GUI_WIDTH), (float) (0), UPGRADE_GUI_WIDTH, UPGRADE_GUI_HEIGHT, 256, 256);
        int height = (int) Math.floor(menu.percentComplete() * BAR_HEIGHT);
        if (height > 0) {
            TeGuiBlit.blit(graphics, TEXTURE, leftPos + PROGRESS_BAR_X, topPos + BAR_EMPTY_Y + BAR_HEIGHT - height, (float) (PROGRESS_BAR_X), (float) (BAR_FULL_Y + BAR_HEIGHT - height), BAR_WIDTH, height, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 6, 6, 0, false);
    }
}
