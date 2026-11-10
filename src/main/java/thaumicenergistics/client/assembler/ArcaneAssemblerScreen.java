package thaumicenergistics.client.assembler;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.assembler.ArcaneAssemblerBlockEntity;
import thaumicenergistics.assembler.ArcaneAssemblerMenu;

public final class ArcaneAssemblerScreen extends AbstractContainerScreen<ArcaneAssemblerMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/arcane.assembler.png");
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
        super(menu, inventory, title);
        imageWidth = MAIN_GUI_WIDTH + UPGRADE_GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int index = 0; index < VIS_BAR_X.length; index++) {
            int left = leftPos + VIS_BAR_X[index];
            int top = topPos + VIS_EMPTY_Y;
            if (mouseX >= left && mouseX < left + VIS_BAR_WIDTH && mouseY >= top && mouseY < top + VIS_BAR_HEIGHT) {
                Aspect aspect = ArcaneAssemblerBlockEntity.PRIMALS[index];
                String tag = aspect.getTag();
                graphics.renderComponentTooltip(font, List.of(
                    Component.literal(Character.toUpperCase(tag.charAt(0)) + tag.substring(1)).withStyle(style -> style.withColor(TextColor.fromRgb(aspect.getColor()))),
                    Component.literal(menu.storedVis(index) / 10.0F + " / 150"),
                    Component.literal(String.format("%.0f%% ", menu.visDiscount(index) * 100.0F)).append(Component.translatable("tc.vis.costavg"))
                ), mouseX, mouseY);
                break;
            }
        }
    }

    private void drawVisBar(GuiGraphics graphics, int x, float percent) {
        int height = (int) Math.floor(percent * VIS_BAR_HEIGHT);
        if (height > 0) {
            graphics.blit(TEXTURE, leftPos + x, topPos + VIS_EMPTY_Y + VIS_BAR_HEIGHT - height, x, VIS_FULL_Y + VIS_BAR_HEIGHT - height, VIS_BAR_WIDTH, height);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, MAIN_GUI_WIDTH, imageHeight);
        graphics.blit(TEXTURE, leftPos + MAIN_GUI_WIDTH, topPos, MAIN_GUI_WIDTH, 0, UPGRADE_GUI_WIDTH, UPGRADE_GUI_HEIGHT);
        for (int index = 0; index < VIS_BAR_X.length; index++) {
            drawVisBar(graphics, VIS_BAR_X[index], menu.storedVis(index) / (float) ArcaneAssemblerBlockEntity.MAX_STORED_CVIS);
        }
        drawVisBar(graphics, PROGRESS_BAR_X, menu.percentComplete());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
    }
}
