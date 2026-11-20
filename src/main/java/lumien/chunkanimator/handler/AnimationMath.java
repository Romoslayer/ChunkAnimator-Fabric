package lumien.chunkanimator.handler;

import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.EasingFunction;
import net.minecraft.core.Direction;

/**
 * Renderer-independent animation maths, shared by the vanilla renderer hooks.
 * The Sodium integration mirrors this logic in GLSL (see {@code SodiumShaderSource}).
 *
 * @author Harley O'Connor
 */
public final class AnimationMath {

    /** How far (in blocks) chunks travel when using one of the horizontal slide modes. */
    public static final float SLIDE_DISTANCE = 200F;

    private AnimationMath() {}

    /**
     * Computes the render offset of a chunk section for the given animation progress.
     *
     * @param mode          the animation mode
     * @param easing        the easing function
     * @param elapsed       milliseconds since the animation started
     * @param duration      total animation duration in milliseconds
     * @param originY       Y coordinate of the section's origin
     * @param facing        the cached facing of the section (only used by horizontal modes)
     * @param horizonHeight the horizon height of the level
     * @param maxY          the exclusive top of the level (min Y + height)
     * @param out           receives the x, y and z offset
     */
    public static void computeOffset(AnimationMode mode, EasingFunction easing, float elapsed, float duration,
                                     int originY, Direction facing, double horizonHeight, int maxY, float[] out) {
        out[0] = 0F;
        out[1] = 0F;
        out[2] = 0F;

        final EasingFunction.Easing ease = easing.easeOutFunc();

        if (mode == AnimationMode.HYBRID) {
            mode = originY < horizonHeight ? AnimationMode.BELOW : AnimationMode.ABOVE;
        }

        switch (mode) {
            case BELOW -> {
                final float distance = Math.abs(originY);
                out[1] = ease.apply(elapsed, 0, distance, duration) - distance;
            }
            case ABOVE -> {
                final float distance = maxY - Math.abs(originY);
                out[1] = distance - ease.apply(elapsed, 0, distance, duration);
            }
            case HORIZONTAL_SLIDE, HORIZONTAL_SLIDE_ALTERNATE -> {
                if (facing != null) {
                    final float mod = -(SLIDE_DISTANCE - ease.apply(elapsed, 0, SLIDE_DISTANCE, duration));
                    out[0] = facing.getStepX() * mod;
                    out[2] = facing.getStepZ() * mod;
                }
            }
            default -> {}
        }
    }

    /**
     * Gets the direction the chunk is facing based on the horizontal distance from the
     * player to the chunk centre.
     */
    public static Direction getChunkFacing(final int difX, final int difZ) {
        return Math.abs(difX) > Math.abs(difZ)
                ? difX > 0 ? Direction.EAST : Direction.WEST
                : difZ > 0 ? Direction.SOUTH : Direction.NORTH;
    }

}
