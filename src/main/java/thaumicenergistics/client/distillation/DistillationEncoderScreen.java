package thaumicenergistics.client.distillation;

import appeng.client.gui.Icon;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.ItemDisplayContext;
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
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 234;
    private static final int TITLE_X = 6;
    private static final int TITLE_Y = 6;
    private static final int BUTTON_ENCODE_X = 146;
    private static final int BUTTON_ENCODE_Y = 94;
    private static final float THAUMOMETER_SCALE = 2.8F;
    private static final float THAUMOMETER_Z = 50.0F;
    private static final int PARTICLE_SIZE = 16;
    private static final int PARTICLE_V = 128;
    private static final int PARTICLE_FRAMES = 16;
    private static final long PARTICLE_TIME = 300L;
    private static final long FRAME_TIME = 1000L / 30L;

    private final ItemStack thaumometer = new ItemStack(ModItems.THAUMOMETER.get());
    private final List<Particle> particles = new ArrayList<>();
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
            checkSourceItem(source);
        }
    }

    private void checkSourceItem(ItemStack source) {
        particles.clear();
        if (source.isEmpty()) {
            return;
        }
        boolean scanned = false;
        List<DistillationAspects.Entry> aspects = menu.sourceAspects();
        if (aspects != null) {
            long now = System.currentTimeMillis();
            for (int index = 0; index < aspects.size(); index++) {
                DistillationAspects.Entry entry = aspects.get(index);
                if (entry.aspect() != null) {
                    scanned = true;
                    particles.add(new Particle(slotY(index), entry.aspect().getColor(), now));
                }
            }
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI((scanned ? ModSounds.HHON : ModSounds.HHOFF).get(), 1.0F));
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
            AspectRenderer.drawTag(graphics, x, y, entry.aspect(), 0.0F);
        } else {
            AspectRenderer.drawUnknown(graphics, x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float rotation = System.currentTimeMillis() % 36000 * 0.02F;
        float scale = THAUMOMETER_SCALE + (float) Math.sin(rotation * 0.15F) * 0.1F;
        renderThaumometer(graphics, rotation, scale);
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

    private void renderThaumometer(GuiGraphics graphics, float rotation, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(leftPos + DistillationEncoderMenu.SLOT_SOURCE_ITEM_X + 8.0F, topPos + DistillationEncoderMenu.SLOT_SOURCE_ITEM_Y - 0.25F + 8.0F, THAUMOMETER_Z);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        pose.scale(16.0F, -16.0F, 16.0F);
        BakedModel model = minecraft.getItemRenderer().getModel(thaumometer, minecraft.level, minecraft.player, 0);
        graphics.flush();
        Lighting.setupForFlatItems();
        minecraft.getItemRenderer().render(thaumometer, ItemDisplayContext.GUI, false, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, 0, false);
        if (!particles.isEmpty()) {
            drawParticles(graphics);
        }
    }

    private void drawParticles(GuiGraphics graphics) {
        long now = System.currentTimeMillis();
        int frame = (int) (now / FRAME_TIME % PARTICLE_FRAMES);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        particles.removeIf(particle -> now - particle.epoch >= PARTICLE_TIME);
        for (Particle particle : particles) {
            double percent = (now - particle.epoch) / (double) PARTICLE_TIME;
            int startX = DistillationEncoderMenu.SLOT_SOURCE_ITEM_X;
            int startY = DistillationEncoderMenu.SLOT_SOURCE_ITEM_Y;
            int x = startX + (int) ((DistillationEncoderMenu.SLOT_SOURCE_ASPECTS_X - startX) * percent);
            int y = startY + (int) ((particle.destY - startY) * percent);
            graphics.setColor((particle.color >> 16 & 0xFF) / 255.0F, (particle.color >> 8 & 0xFF) / 255.0F, (particle.color & 0xFF) / 255.0F, 0.9F);
            graphics.blit(PARTICLES, x, y, frame * PARTICLE_SIZE, PARTICLE_V, PARTICLE_SIZE, PARTICLE_SIZE);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        graphics.pose().popPose();
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

    private record Particle(int destY, int color, long epoch) {
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
            int x = getX();
            int y = getY();
            graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
            graphics.fill(x, y, x + 15, y + 15, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + 15, y + 15, 0xFF8B8B8B);
            Icon.WHITE_ARROW_DOWN.getBlitter().dest(x, y).blit(graphics);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
