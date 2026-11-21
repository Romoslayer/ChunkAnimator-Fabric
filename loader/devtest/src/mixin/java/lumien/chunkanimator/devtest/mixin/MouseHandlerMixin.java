package lumien.chunkanimator.devtest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development-only: automated test runs must never capture the mouse, so whoever is using the
 * computer keeps their cursor while the game window is open.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void chunkanimator$neverGrabMouse(final CallbackInfo ci) {
        ci.cancel();
    }

}
