package thaumicenergistics.client.distillation;

import appeng.client.gui.Icon;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.aspect.AspectRenderer;
import thaumcraft.registry.ModItems;
import thaumcraft.registry.ModSounds;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.distillation.DistillationAspects;
import thaumicenergistics.distillation.DistillationEncoderMenu;

public final class DistillationEncoderScreen extends AbstractContainerScreen<DistillationEncoderMenu> {
    private static final ResourceLocation TEXTURE = ThaumicEnergistics.id("textures/gui/distillation.encoder.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 234;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 6;
    private static final int BUTTON_ENCODE_X = 146;
    private static final int BUTTON_ENCODE_Y = 94;
    private static final float THAUMOMETER_SCALE = 2.8F;

    private final ItemStack thaumometer = new ItemStack(ModItems.THAUMOMETER.get());
    private ItemStack lastSource = ItemStack.EMPTY;

    public DistillationEncoderScreen(DistillationEncoderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new EncodeButton(leftPos + BUTTON_ENCODE_X, topPos + BUTTON_ENCODE_Y));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack source = menu.sourceItem();
        if (!ItemStack.matches(source, lastSource)) {
            lastSource = source.copy();
            if (!source.isEmpty()) {
                List<DistillationAspects.Entry> aspects = menu.sourceAspects();
                boolean scanned = aspects != null && !aspects.isEmpty() && aspects.get(0).aspect() != null;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI((scanned ? ModSounds.HHON : ModSounds.HHOFF).get(), 1.0F));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static int slotX(int index) {
        return DistillationEncoderMenu.SLOT_SOURCE_ASPECTS_X;
    }

    private static int slotY(int index) {
        return DistillationEncoderMenu.SLOT_SOURCE_ASPECTS_Y + index * 18;
    }

    private void drawEntry(GuiGraphics graphics, int x, int y, DistillationAspects.Entry entry) {
        if (entry.aspect() != null && DistillationAspects.canSee(minecraft.player, entry.aspect())) {
            AspectRenderer.drawTag(graphics, x, y, entry.aspect(), entry.amount());
        } else {
            AspectRenderer.drawUnknown(graphics, x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float rotation = System.currentTimeMillis() % 36000 * 0.02F;
        float scale = THAUMOMETER_SCALE + (float) Math.sin(rotation * 0.15F) * 0.1F;
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + DistillationEncoderMenu.SLOT_SOURCE_ITEM_X + 8.0F, topPos + DistillationEncoderMenu.SLOT_SOURCE_ITEM_Y - 0.25F + 8.0F, -150.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        graphics.renderItem(thaumometer, -8, -8);
        graphics.pose().popPose();
        List<DistillationAspects.Entry> aspects = menu.sourceAspects();
        if (aspects != null) {
            for (int index = 0; index < aspects.size(); index++) {
                drawEntry(graphics, leftPos + slotX(index), topPos + slotY(index), aspects.get(index));
            }
        }
        DistillationAspects.Entry selected = menu.selectedAspect();
        if (selected != null) {
            drawEntry(graphics, leftPos + DistillationEncoderMenu.SLOT_SELECTED_ASPECT_X, topPos + DistillationEncoderMenu.SLOT_SELECTED_ASPECT_Y, selected);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        List<DistillationAspects.Entry> aspects = menu.sourceAspects();
        if (aspects == null) {
            return;
        }
        for (int index = 0; index < aspects.size(); index++) {
            DistillationAspects.Entry entry = aspects.get(index);
            if (isHovering(slotX(index), slotY(index), 16, 16, mouseX, mouseY)) {
                boolean known = entry.aspect() != null && DistillationAspects.canSee(minecraft.player, entry.aspect());
                graphics.renderTooltip(font, known ? Component.literal(entry.aspect().getName()) : Component.translatable("tc.aspect.unknown"), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<DistillationAspects.Entry> aspects = menu.sourceAspects();
        if (aspects != null) {
            for (int index = 0; index < aspects.size(); index++) {
                if (isHovering(slotX(index), slotY(index), 18, 18, mouseX, mouseY)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    press(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void press(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private final class EncodeButton extends AbstractButton {
        EncodeButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
        }

        @Override
        public void onPress() {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DistillationEncoderMenu.BUTTON_ENCODE);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(getX(), getY()).blit(graphics);
            Icon.WHITE_ARROW_DOWN.getBlitter().dest(getX(), getY()).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
