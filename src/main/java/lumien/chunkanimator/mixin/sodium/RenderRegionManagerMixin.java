package lumien.chunkanimator.mixin.sodium;

import lumien.chunkanimator.compat.SodiumCompat;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Sodium never records a "first built" time for sections within ~28 blocks of the player, so
 * they would never animate. Replace that radius with Chunk Animator's own setting.
 */
@Mixin(value = RenderRegionManager.class, remap = false)
public abstract class RenderRegionManagerMixin {

    @ModifyConstant(method = "uploadResults", constant = @Constant(doubleValue = 768.0), require = 0)
    private double chunkanimator$nearbyDistance(double original) {
        return SodiumCompat.nearbyDistanceSquared();
    }

}
