package thaumicenergistics.client.platform;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class TeRenderTypes {
    private TeRenderTypes() {
    }

    public static RenderType entityCutout(Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    public static RenderType entityTranslucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType text(Identifier texture) {
        return RenderTypes.text(texture);
    }
}
