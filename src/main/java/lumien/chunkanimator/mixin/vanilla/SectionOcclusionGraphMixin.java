package lumien.chunkanimator.mixin.vanilla;

import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.handler.AnimatedSection;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks sections that stop being empty (the player placed the first block in them) so the new
 * geometry does not animate in as if the chunk had just loaded.
 */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {

    @Inject(method = "updateEmptySections", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;schedulePropagationFrom(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;)V"
    ))
    private void chunkanimator$markNoLongerEmpty(CallbackInfo ci, @Local SectionRenderDispatcher.RenderSection section) {
        ((AnimatedSection) section).chunkanimator$markSkip();
    }

}
