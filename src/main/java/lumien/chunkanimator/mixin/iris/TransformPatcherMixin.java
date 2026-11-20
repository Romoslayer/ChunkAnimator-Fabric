package lumien.chunkanimator.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import lumien.chunkanimator.shader.ShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Patches the terrain shaders Iris builds from a shader pack for Sodium, so chunks animate with
 * shader packs on too (shadows included). Iris is not a compile dependency: the target is named
 * by string and the result map is handled generically.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
public abstract class TransformPatcherMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ModifyReturnValue(method = "patchSodium", at = @At("RETURN"), require = 0)
    private static Map chunkanimator$patchSodiumTerrain(Map original) {
        return ShaderPatcher.patchIrisSodium(original);
    }

}
