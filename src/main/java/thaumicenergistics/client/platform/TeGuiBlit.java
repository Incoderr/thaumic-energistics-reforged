package thaumicenergistics.client.platform;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class TeGuiBlit {
    private TeGuiBlit() {
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight);
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }
}
