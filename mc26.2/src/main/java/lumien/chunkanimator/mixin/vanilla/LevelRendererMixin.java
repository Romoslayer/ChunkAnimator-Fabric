package lumien.chunkanimator.mixin.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.handler.AnimatedSection;
import lumien.chunkanimator.handler.AnimationHandler;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.2: offsets each animating section's per-draw uniform data.
 *
 * @author Harley O'Connor
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @WrapOperation(method = "prepareChunkRenders", at = @At(
            value = "NEW",
            target = "(Lorg/joml/Matrix4fc;IIIFII)Lnet/minecraft/client/renderer/DynamicUniforms$ChunkSectionInfo;"
    ))
    private DynamicUniforms.ChunkSectionInfo chunkanimator$animateSection(Matrix4fc modelView, int x, int y, int z, float visibility,
                                                                         int atlasWidth, int atlasHeight,
                                                                         Operation<DynamicUniforms.ChunkSectionInfo> original,
                                                                         @Local SectionRenderDispatcher.RenderSection section) {
        final AnimationHandler.SectionTransform transform = ChunkAnimator.handler().apply((AnimatedSection) section, x, y, z, visibility);
        if (transform == null) {
            return original.call(modelView, x, y, z, visibility, atlasWidth, atlasHeight);
        }
        return original.call(modelView, transform.x, transform.y, transform.z, transform.visibility, atlasWidth, atlasHeight);
    }

}
