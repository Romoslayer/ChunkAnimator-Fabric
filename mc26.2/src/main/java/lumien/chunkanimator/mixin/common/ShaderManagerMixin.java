package lumien.chunkanimator.mixin.common;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.2: shaders are stored with their imports already inlined, so both the shader
 * body and any imported blocks (like Sodium's globals) are patched here in one go.
 */
@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {

    @WrapOperation(method = "loadShader", at = @At(
            value = "INVOKE",
            target = "Lcom/google/common/collect/ImmutableMap$Builder;put(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap$Builder;"
    ))
    private static ImmutableMap.Builder<Object, Object> chunkanimator$patchShader(ImmutableMap.Builder<Object, Object> builder, Object key, Object source,
                                                                                  Operation<ImmutableMap.Builder<Object, Object>> original,
                                                                                  @Local(argsOnly = true) Identifier location) {
        final Object patched = source instanceof String text ? ShaderPatcher.patch(location.getNamespace(), location.getPath(), text) : source;
        return original.call(builder, key, patched);
    }

}
