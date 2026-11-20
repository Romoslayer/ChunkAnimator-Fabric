package lumien.chunkanimator.shader;

/**
 * GLSL injected into Minecraft's terrain shaders. See {@link ShaderPatcher#packVisibility}.
 */
final class VanillaShaderSource {

    private VanillaShaderSource() {}

    /**
     * Unpacks {@code visibility + 2 * (1 + axis * 256 + fraction)} into
     * {@code vec4(sub-block offset xyz, visibility)}. Plain values (0..1) pass straight through.
     */
    static final String DECODE_FUNCTION = """
            #ifndef CHUNKANIM_DECODE
            #define CHUNKANIM_DECODE
            // Chunk Animator: decode the sub-block animation offset packed into the section visibility.
            vec4 chunkanim_decode(float packedVisibility) {
                if (packedVisibility < 1.5) {
                    return vec4(0.0, 0.0, 0.0, packedVisibility);
                }
                float packedOffset = floor(packedVisibility * 0.5);
                float visibility = packedVisibility - 2.0 * packedOffset;
                packedOffset -= 1.0;
                float axis = floor(packedOffset / 256.0);
                float fraction = (packedOffset - axis * 256.0) / 256.0;
                vec3 offset = axis < 0.5 ? vec3(fraction, 0.0, 0.0) : (axis < 1.5 ? vec3(0.0, fraction, 0.0) : vec3(0.0, 0.0, fraction));
                return vec4(offset, visibility);
            }
            #endif
            """;

}
