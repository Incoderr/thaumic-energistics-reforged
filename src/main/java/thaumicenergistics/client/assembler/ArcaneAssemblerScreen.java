package thaumicenergistics.client.assembler;

import thaumicenergistics.client.platform.TeGuiBlit;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.assembler.ArcaneAssemblerBlockEntity;
import thaumicenergistics.assembler.ArcaneAssemblerMenu;

public final class ArcaneAssemblerScreen extends AbstractContainerScreen<ArcaneAssemblerMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/arcane.assembler.png");
    private static final int MAIN_GUI_WIDTH = 176;
    private static final int UPGRADE_GUI_WIDTH = 234 - MAIN_GUI_WIDTH;
    private static final int GUI_HEIGHT = 197;
    private static final int UPGRADE_GUI_HEIGHT = 104;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 6;
    private static final int VIS_EMPTY_Y = 87;
    private static final int VIS_FULL_Y = 202;
    private static final int VIS_BAR_HEIGHT = 16;
    private static final int VIS_BAR_WIDTH = 4;
    private static final int[] VIS_BAR_X = {41, 59, 77, 95, 113, 131};
    private static final int PROGRESS_BAR_X = 149;

    public ArcaneAssemblerScreen(ArcaneAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MAIN_GUI_WIDTH + UPGRADE_GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (int index = 0; index < VIS_BAR_X.length; index++) {
            int left = leftPos + VIS_BAR_X[index];
            int top = topPos + VIS_EMPTY_Y;
            if (mouseX >= left && mouseX < left + VIS_BAR_WIDTH && mouseY >= top && mouseY < top + VIS_BAR_HEIGHT) {
                Aspect aspect = ArcaneAssemblerBlockEntity.PRIMALS[index];
                String tag = aspect.getTag();
                graphics.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(Character.toUpperCase(tag.charAt(0)) + tag.substring(1)).withStyle(style -> style.withColor(TextColor.fromRgb(aspect.getColor()))),
                    Component.literal(menu.storedVis(index) / 10.0F + " / 150"),
                    Component.literal(String.format("%.0f%% ", menu.visDiscount(index) * 100.0F)).append(Component.translatable("tc.vis.costavg"))
                ), mouseX, mouseY);
                break;
            }
        }
    }

    private void drawVisBar(GuiGraphicsExtractor graphics, int x, float percent) {
        int height = (int) Math.floor(percent * VIS_BAR_HEIGHT);
        if (height > 0) {
            TeGuiBlit.blit(graphics, TEXTURE, leftPos + x, topPos + VIS_EMPTY_Y + VIS_BAR_HEIGHT - height, (float) (x), (float) (VIS_FULL_Y + VIS_BAR_HEIGHT - height), VIS_BAR_WIDTH, height, 256, 256);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), MAIN_GUI_WIDTH, imageHeight, 256, 256);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos + MAIN_GUI_WIDTH, topPos, (float) (MAIN_GUI_WIDTH), (float) (0), UPGRADE_GUI_WIDTH, UPGRADE_GUI_HEIGHT, 256, 256);
        for (int index = 0; index < VIS_BAR_X.length; index++) {
            drawVisBar(graphics, VIS_BAR_X[index], menu.storedVis(index) / (float) ArcaneAssemblerBlockEntity.MAX_STORED_CVIS);
        }
        drawVisBar(graphics, PROGRESS_BAR_X, menu.percentComplete());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, TITLE_X, TITLE_Y, 0xFF000000, false);
    }
}
