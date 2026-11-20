package lumien.chunkanimator.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.Std140Builder;
import lumien.chunkanimator.compat.SodiumCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Appends Chunk Animator's settings to the end of Sodium's per-frame terrain globals
 * (the block is allocated at 256 bytes, of which Sodium uses 184).
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager$GlobalUniforms")
public abstract class GlobalUniformsMixin {

    @ModifyExpressionValue(method = "write", require = 0, at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putInt(I)Lcom/mojang/blaze3d/buffers/Std140Builder;"
    ))
    private Std140Builder chunkanimator$appendSettings(Std140Builder builder) {
        return SodiumCompat.appendGlobals(builder);
    }

}
