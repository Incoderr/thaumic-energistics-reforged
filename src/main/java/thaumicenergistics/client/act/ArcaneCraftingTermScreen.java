package thaumicenergistics.client.act;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.client.aspect.AspectRenderer;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.act.ArcaneCraftingTermMenu;

public final class ArcaneCraftingTermScreen extends MEStorageScreen<ArcaneCraftingTermMenu> {
    public static final String STYLE = "/screens/thaumicenergistics/arcane_crafting_terminal.json";
    private static final ResourceLocation STATES = ThaumicEnergistics.id("textures/gui/states.png");
    private static final int GUI_HEIGHT = 243;
    private static final int ASPECT_COST_POS_X = 136;
    private static final int ASPECT_COST_POS_Y = 90;
    private static final int ASPECT_COST_SPACING = 18;
    private static final double ASPECT_COST_BLINK_SPEED = 0.5;
    private static final float ASPECT_COST_MIN_ALPHA = 0.25F;
    private static final float ASPECT_COST_MAX_ALPHA = 0.75F;

    public ArcaneCraftingTermScreen(ArcaneCraftingTermMenu menu, Inventory inventory, Component title, ScreenStyle style) {
        super(menu, inventory, title, style);
        ActionButton clear = new ActionButton(ActionItems.S_STASH, button -> menu.clearCraftingGrid());
        clear.setHalfSize(true);
        clear.setDisableBackground(true);
        widgets.add("clearCraftingGrid", clear);
        widgets.add("swapArmor", new SwapArmorButton(menu));
    }

    @Override
    public void drawFG(GuiGraphics graphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(graphics, offsetX, offsetY, mouseX, mouseY);
        int lowerOffset = imageHeight - GUI_HEIGHT;
        int column = 0;
        int y = ASPECT_COST_POS_Y + lowerOffset;
        for (ArcaneCraftingTermMenu.Cost cost : menu.clientCosts()) {
            float alpha = cost.enough() ? 1.0F : blinkAlpha();
            AspectRenderer.drawTag(graphics, ASPECT_COST_POS_X + column * ASPECT_COST_SPACING, y, cost.aspect(), cost.vis(), 0, alpha, false);
            if (++column == 2) {
                column = 0;
                y += ASPECT_COST_SPACING;
            }
        }
    }

    private static float blinkAlpha() {
        double phase = System.currentTimeMillis() / 1000.0 * ASPECT_COST_BLINK_SPEED % 2.0;
        double wave = phase > 1.0 ? 2.0 - phase : phase;
        return (float) (ASPECT_COST_MIN_ALPHA + (ASPECT_COST_MAX_ALPHA - ASPECT_COST_MIN_ALPHA) * wave);
    }

    private static final class SwapArmorButton extends AbstractButton {
        private final ArcaneCraftingTermMenu menu;

        SwapArmorButton(ArcaneCraftingTermMenu menu) {
            super(0, 0, 8, 8, Component.empty());
            this.menu = menu;
            setTooltip(Tooltip.create(Component.translatable("thaumicenergistics.tooltip.button.swap.armor.title")
                .append("\n")
                .append(Component.translatable("thaumicenergistics.tooltip.button.swap.armor").withStyle(ChatFormatting.GRAY))));
        }

        @Override
        public void onPress() {
            menu.swapArmor();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY(), width, height).blit(graphics);
            graphics.blit(STATES, getX(), getY(), width, height, 0, 0, 16, 16, 256, 256);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
