package lumien.chunkanimator.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.compat.SodiumCompat;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sodium skips block faces that point away from the camera. A section that is mid-animation
 * is drawn somewhere other than its real position (e.g. far below or above the camera), so
 * faces Sodium would cull can be visible. Draw every face of animating sections.
 */
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererMixin {

    @WrapOperation(method = "fillCommandBuffer", require = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;getVisibleFaces(IIIIII)I"
    ))
    private static int chunkanimator$drawAllFacesWhileAnimating(int originX, int originY, int originZ,
                                                                 int chunkX, int chunkY, int chunkZ,
                                                                 Operation<Integer> original,
                                                                 @Local(argsOnly = true) RenderRegion region) {
        final int sectionIndex = LocalSectionIndex.pack(chunkX - region.getChunkX(), chunkY - region.getChunkY(), chunkZ - region.getChunkZ());
        if (SodiumCompat.isAnimating(region.getId(), region.getCreationTime(), sectionIndex)) {
            return ModelQuadFacing.ALL;
        }
        return original.call(originX, originY, originZ, chunkX, chunkY, chunkZ);
    }

}
