package thaumicenergistics.client.infusion;

import thaumicenergistics.client.platform.TeGuiBlit;
import net.minecraft.client.input.InputWithModifiers;
import appeng.client.gui.style.Blitter;
import appeng.util.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.aspect.Aspect;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.crafting.InfusionRecipe;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.infusion.InfusionInscriberMenu;

public final class InfusionInscriberScreen extends AbstractContainerScreen<InfusionInscriberMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/infusion.inscriber.png");
    private static final int GUI_WIDTH = 210;
    private static final int GUI_HEIGHT = 244;
    private static final int ASPECTS_X = 98;
    private static final int ASPECTS_Y = 126;
    private static final int ASPECT_SPACING = 16;
    private static final int ASPECTS_PER_ROW = 6;

    private SaveButton saveButton;

    public InfusionInscriberScreen(InfusionInscriberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        saveButton = addRenderableWidget(new SaveButton(leftPos + 141, topPos + 109));
        addRenderableWidget(new NextButton(leftPos + 80, topPos + 89));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        saveButton.update(menu.saveState());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 6, 6, 0xFF000000, false);
        InfusionRecipe recipe = menu.selected();
        if (recipe == null) {
            return;
        }
        int index = 0;
        for (Aspect aspect : recipe.aspects().getAspects()) {
            int x = ASPECTS_X + index % ASPECTS_PER_ROW * ASPECT_SPACING;
            int y = ASPECTS_Y + index / ASPECTS_PER_ROW * ASPECT_SPACING;
            AspectRenderer.drawTag(graphics, x, y, aspect, recipe.aspects().getAmount(aspect));
            index++;
        }
        int count = menu.recipes().size();
        if (count > 1) {
            graphics.text(font, (menu.recipes().indexOf(recipe) + 1) + "/" + count, 92, 90, 0xFF404040, false);
        }
    }

    private void press(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private final class SaveButton extends AbstractButton {
        private InfusionInscriberMenu.SaveState state = InfusionInscriberMenu.SaveState.MISSING_CORE;

        SaveButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
            update(state);
        }

        void update(InfusionInscriberMenu.SaveState newState) {
            state = newState;
            active = state == InfusionInscriberMenu.SaveState.SAVE || state == InfusionInscriberMenu.SaveState.DELETE;
            String key = switch (state) {
                case INVALID_RECIPE -> "invalid";
                case CORE_FULL -> "full";
                case MISSING_CORE -> "missing";
                case SAVE -> "save";
                case DELETE -> "delete";
            };
            setTooltip(Tooltip.create(Component.translatable("thaumicenergistics.tooltip.button.infusion." + key)));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            press(InfusionInscriberMenu.BUTTON_SAVE_DELETE);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Blitter.icon(Icon.TOOLBAR_BUTTON_BACKGROUND).dest(getX(), getY()).blit(graphics);
            if (state == InfusionInscriberMenu.SaveState.SAVE) {
                Blitter.icon(Icon.WHITE_ARROW_DOWN).dest(getX(), getY()).blit(graphics);
            } else if (state == InfusionInscriberMenu.SaveState.DELETE) {
                Blitter.icon(Icon.CLEAR).dest(getX(), getY()).blit(graphics);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class NextButton extends AbstractButton {
        NextButton(int x, int y) {
            super(x, y, 8, 8, Component.empty());
            setTooltip(Tooltip.create(Component.translatable("thaumicenergistics.tooltip.button.infusion.next")));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            press(InfusionInscriberMenu.BUTTON_NEXT);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Blitter.icon(Icon.ARROW_RIGHT).dest(getX(), getY(), width, height).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
