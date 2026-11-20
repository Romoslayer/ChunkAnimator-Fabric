package lumien.chunkanimator.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Minecraft 26.3: every {@code #include}d GLSL file passes through here, which is where Sodium's
 * globals block gets extended.
 */
@Mixin(ShaderSource.CachedIncludeSource.class)
public abstract class CachedIncludeSourceMixin {

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String chunkanimator$patchInclude(String source, @Local(argsOnly = true) Identifier id) {
        return ShaderPatcher.patch(id.getNamespace(), id.getPath(), source);
    }

}
