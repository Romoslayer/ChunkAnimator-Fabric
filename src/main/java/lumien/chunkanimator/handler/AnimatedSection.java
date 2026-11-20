package lumien.chunkanimator.handler;

import net.minecraft.core.Direction;

/**
 * Animation state that is mixed into vanilla's {@code SectionRenderDispatcher.RenderSection}.
 * Render sections are pooled and re-used for different positions, so the state is reset
 * whenever vanilla resets the section.
 */
public interface AnimatedSection {

    /** Not yet drawn with geometry since the last reset. */
    long NOT_STARTED = 0L;
    /** Animation finished, or was never wanted for the current contents. */
    long DONE = -1L;

    long chunkanimator$getAnimationStart();

    void chunkanimator$setAnimationStart(long start);

    /**
     * True if this section's geometry appeared because blocks were placed in a previously empty
     * section, rather than because the chunk was loaded. Those should not animate.
     */
    boolean chunkanimator$shouldSkip();

    void chunkanimator$markSkip();

    Direction chunkanimator$getFacing();

    void chunkanimator$setFacing(Direction facing);

}
