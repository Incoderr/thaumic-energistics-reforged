package thaumicenergistics.mixin;

import appeng.api.stacks.AEKey;
import appeng.menu.me.common.MEStorageMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicenergistics.essentia.EssentiaKey;

@Mixin(value = MEStorageMenu.class, remap = false)
public abstract class MEStorageMenuMixin {
    @Inject(method = "isKeyVisible", at = @At("HEAD"), cancellable = true)
    private void thaumicenergistics$hideEssentia(AEKey key, CallbackInfoReturnable<Boolean> callback) {
        if (key instanceof EssentiaKey) {
            callback.setReturnValue(false);
        }
    }
}
