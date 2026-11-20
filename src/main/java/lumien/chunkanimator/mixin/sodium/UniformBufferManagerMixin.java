package lumien.chunkanimator.mixin.sodium;

import lumien.chunkanimator.compat.SodiumCompat;
import net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors Sodium's GPU-side section timestamps on the CPU, so we know which sections are
 * currently mid-animation.
 */
@Mixin(value = UniformBufferManager.class, remap = false)
public abstract class UniformBufferManagerMixin {

    @Inject(method = "writeMeshTimes", at = @At("HEAD"), require = 0)
    private void chunkanimator$recordMeshTime(int id, int sectionIndex, int relativeBuiltTime, CallbackInfo ci) {
        SodiumCompat.recordSectionTime(id, sectionIndex, relativeBuiltTime);
    }

    @Inject(method = "clearRegionTimes", at = @At("HEAD"), require = 0)
    private void chunkanimator$clearRegionTimes(int id, CallbackInfo ci) {
        SodiumCompat.clearRegion(id);
    }

    @Inject(method = "delete", at = @At("HEAD"), require = 0)
    private void chunkanimator$delete(CallbackInfo ci) {
        SodiumCompat.clearAll();
    }

}
