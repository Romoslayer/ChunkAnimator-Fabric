package lumien.chunkanimator.mixin.vanilla;

import lumien.chunkanimator.handler.AnimatedSection;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores animation state directly on vanilla's pooled render sections, replacing the
 * {@code WeakHashMap} the original mod used.
 *
 * @author Harley O'Connor
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMixin implements AnimatedSection {

    @Shadow
    private long uploadedTime;

    @Unique
    private volatile long chunkanimator$animationStart = AnimatedSection.NOT_STARTED;
    @Unique
    private volatile boolean chunkanimator$skip;
    @Unique
    private volatile Direction chunkanimator$facing;

    /** Called whenever the section is cleared, including when it is moved to a new position. */
    @Inject(method = "reset", at = @At("TAIL"))
    private void chunkanimator$reset(CallbackInfo ci) {
        this.chunkanimator$animationStart = AnimatedSection.NOT_STARTED;
        this.chunkanimator$skip = false;
        this.chunkanimator$facing = null;
    }

    /**
     * If the first mesh compiled for this position is empty, any geometry that shows up later
     * comes from blocks being placed, which should not animate.
     */
    @Inject(method = "setSectionMesh", at = @At("HEAD"))
    private void chunkanimator$onSetSectionMesh(SectionMesh mesh, CallbackInfoReturnable<SectionMesh> cir) {
        if (this.uploadedTime == 0L && !mesh.hasRenderableLayers()) {
            this.chunkanimator$skip = true;
        }
    }

    @Override
    public long chunkanimator$getAnimationStart() {
        return this.chunkanimator$animationStart;
    }

    @Override
    public void chunkanimator$setAnimationStart(long start) {
        this.chunkanimator$animationStart = start;
    }

    @Override
    public boolean chunkanimator$shouldSkip() {
        return this.chunkanimator$skip;
    }

    @Override
    public void chunkanimator$markSkip() {
        this.chunkanimator$skip = true;
    }

    @Override
    public Direction chunkanimator$getFacing() {
        return this.chunkanimator$facing;
    }

    @Override
    public void chunkanimator$setFacing(Direction facing) {
        this.chunkanimator$facing = facing;
    }

}
