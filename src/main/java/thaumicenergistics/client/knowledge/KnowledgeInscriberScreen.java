package thaumicenergistics.client.knowledge;

import thaumicenergistics.client.platform.TeGuiBlit;
import net.minecraft.util.ARGB;
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
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.knowledge.KnowledgeInscriberMenu;

public final class KnowledgeInscriberScreen extends AbstractContainerScreen<KnowledgeInscriberMenu> {
    private static final Identifier TEXTURE = ThaumicEnergistics.id("textures/gui/knowledge.inscriber.png");
    private static final Identifier BACKGROUND = ThaumicEnergistics.id("textures/research/research.background.png");
    private static final int GUI_WIDTH = 210;
    private static final int GUI_HEIGHT = 244;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 6;
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final int GRID_SIZE = 3;
    private static final int PARTICLE_SIZE = 16;
    private static final int PARTICLE_V = 64;
    private static final int PARTICLE_FRAMES = 4;
    private static final long PARTICLE_TIME = 400L;
    private static final long PARTICLE_DELAY = 50L;
    private static final long FRAME_TIME = 1000L;

    private final long[] particleEpochs = new long[GRID_SIZE * GRID_SIZE];
    private int lastSaveCounter = -1;
    private boolean hasParticlesToDraw;

    private SaveButton saveButton;

    public KnowledgeInscriberScreen(KnowledgeInscriberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, GUI_HEIGHT);
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
        int counter = menu.saveCounter();
        if (lastSaveCounter >= 0 && counter != lastSaveCounter) {
            long now = System.currentTimeMillis();
            for (int index = 0; index < particleEpochs.length; index++) {
                particleEpochs[index] = now + index * PARTICLE_DELAY;
            }
            hasParticlesToDraw = true;
        }
        lastSaveCounter = counter;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        long slowTime = System.currentTimeMillis() / 100;
        float red = Math.abs(1.0F - slowTime % 50 / 50.0F * 2.0F);
        TeGuiBlit.blit(graphics, BACKGROUND, leftPos + 80, topPos + 106, (float) (55 - (int) (slowTime % 10)), (float) (25), 35, 20, 256, 256, ARGB.colorFromFloat(1.0F, red, 1.0F - red, 1.0F));
        TeGuiBlit.blit(graphics, TEXTURE, leftPos, topPos, (float) (0), (float) (0), imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, TITLE_X, TITLE_Y, 0xFF000000, false);
        if (hasParticlesToDraw) {
            drawSaveParticles(graphics);
        }
    }

    private void drawSaveParticles(GuiGraphicsExtractor graphics) {
        hasParticlesToDraw = false;
        long now = System.currentTimeMillis();
        int frame = (int) (now / FRAME_TIME % PARTICLE_FRAMES);
        for (int index = 0; index < particleEpochs.length; index++) {
            double percent = (now - particleEpochs[index]) / (double) PARTICLE_TIME;
            if (percent >= 1.0D) {
                continue;
            }
            hasParticlesToDraw = true;
            if (percent < 0) {
                continue;
            }
            int startX = KnowledgeInscriberMenu.CRAFTING_SLOT_X + index % GRID_SIZE * KnowledgeInscriberMenu.SLOT_SPACING;
            int startY = KnowledgeInscriberMenu.CRAFTING_SLOT_Y + index / GRID_SIZE * KnowledgeInscriberMenu.SLOT_SPACING;
            int x = startX + (int) ((KnowledgeInscriberMenu.KCORE_SLOT_X - startX) * percent);
            int y = startY + (int) ((KnowledgeInscriberMenu.KCORE_SLOT_Y - startY) * percent);
            TeGuiBlit.blit(graphics, PARTICLES, x, y, (float) (frame * PARTICLE_SIZE), (float) (PARTICLE_V), PARTICLE_SIZE, PARTICLE_SIZE, 256, 256, ARGB.colorFromFloat(0.9F, 0.2F, 0.5F, 1.0F));
        }
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
        public void onPress(InputWithModifiers input) {
            press(KnowledgeInscriberMenu.BUTTON_SAVE_DELETE);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Blitter.icon(Icon.TOOLBAR_BUTTON_BACKGROUND).dest(getX(), getY()).blit(graphics);
            if (state == KnowledgeInscriberMenu.SaveState.SAVE) {
                Blitter.icon(Icon.WHITE_ARROW_DOWN).dest(getX(), getY()).blit(graphics);
            } else if (state == KnowledgeInscriberMenu.SaveState.DELETE) {
                Blitter.icon(Icon.CLEAR).dest(getX(), getY()).blit(graphics);
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
        public void onPress(InputWithModifiers input) {
            press(KnowledgeInscriberMenu.BUTTON_CLEAR);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Blitter.icon(Icon.S_CLEAR).dest(getX(), getY(), width, height).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
