package lumien.chunkanimator.config;

import penner.easing.*;

/**
 * The easing functions a chunk can use while moving into place.
 *
 * <p>The ordinal of each constant is shared with the GLSL implementation used by the Sodium
 * integration ({@code assets/chunkanimator/shaders/sodium_animation.glsl}), so the order must
 * not be changed without updating that file too.</p>
 *
 * @author Harley O'Connor
 */
public enum EasingFunction {
    LINEAR(Linear::easeOut),
    QUAD(Quad::easeOut),
    CUBIC(Cubic::easeOut),
    QUART(Quart::easeOut),
    QUINT(Quint::easeOut),
    EXPO(Expo::easeOut),
    SINE(Sine::easeOut),
    CIRC(Circ::easeOut),
    BACK(Back::easeOut),
    BOUNCE(Bounce::easeOut),
    ELASTIC(Elastic::easeOut);

    private final Easing easeOutFunc;

    EasingFunction(Easing easeOutFunc) {
        this.easeOutFunc = easeOutFunc;
    }

    public Easing easeOutFunc() {
        return easeOutFunc;
    }

    /**
     * Penner-style easing signature.
     */
    @FunctionalInterface
    public interface Easing {
        /**
         * @param t current time
         * @param b start value
         * @param c total change in value
         * @param d total duration
         */
        float apply(float t, float b, float c, float d);
    }
}
