package lumien.chunkanimator.config;

/**
 * Controls how chunks are animated when they appear.
 *
 * <p>The ordinal of each constant is shared with the GLSL implementation used by the Sodium
 * integration, so the order must not be changed without updating that shader too.</p>
 *
 * @author Harley O'Connor
 */
public enum AnimationMode {
    /** Chunks always appear from below. */
    BELOW,
    /** Chunks always appear from above. */
    ABOVE,
    /** Chunks below the horizon appear from below, the rest from above. */
    HYBRID,
    /** Chunks slide in horizontally from their cardinal direction relative to the player. */
    HORIZONTAL_SLIDE,
    /** Like {@link #HORIZONTAL_SLIDE}, but the direction is decided when the chunk first renders. */
    HORIZONTAL_SLIDE_ALTERNATE;

    public boolean isHorizontal() {
        return this == HORIZONTAL_SLIDE || this == HORIZONTAL_SLIDE_ALTERNATE;
    }
}
