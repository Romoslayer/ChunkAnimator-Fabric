package lumien.chunkanimator.mixin.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.handler.AnimatedSection;
import lumien.chunkanimator.handler.AnimationHandler;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.3: offsets each animating section's GPU data. The same record feeds both the
 * per-draw uniform path and the multi-draw-indirect instanced path, so one hook covers both.
 *
 * @author Harley O'Connor
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @WrapOperation(method = "extractSectionDrawGroups", at = @At(
            value = "NEW",
            target = "(IIIF)Lnet/minecraft/client/renderer/DynamicGpuData$ChunkSectionInfo;"
    ))
    private DynamicGpuData.ChunkSectionInfo chunkanimator$animateSection(int x, int y, int z, float visibility,
                                                                        Operation<DynamicGpuData.ChunkSectionInfo> original,
                                                                        @Local SectionRenderDispatcher.RenderSection section) {
        final AnimationHandler.SectionTransform transform = ChunkAnimator.handler().apply((AnimatedSection) section, x, y, z, visibility);
        if (transform == null) {
            return original.call(x, y, z, visibility);
        }
        return original.call(transform.x, transform.y, transform.z, transform.visibility);
    }

}
