package lumien.chunkanimator.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.3: patches shader bodies as they are read. Includes are handled separately by
 * {@link CachedIncludeSourceMixin}.
 */
@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {

    @ModifyExpressionValue(method = "loadShader", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"
    ))
    private static String chunkanimator$patchShader(String source, @Local(argsOnly = true) Identifier location) {
        return ShaderPatcher.patch(location.getNamespace(), location.getPath(), source);
    }

}
