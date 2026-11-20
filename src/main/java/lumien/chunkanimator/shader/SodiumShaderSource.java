package lumien.chunkanimator.shader;

/**
 * GLSL injected into Sodium's terrain shader. Mirrors
 * {@link lumien.chunkanimator.handler.AnimationMath} and the Penner easing functions.
 *
 * <p>Mode and easing ids are the ordinals of
 * {@link lumien.chunkanimator.config.AnimationMode} and
 * {@link lumien.chunkanimator.config.EasingFunction}.</p>
 */
public final class SodiumShaderSource {

    /** Written into {@code u_ChunkAnimParams.w} so the shader can tell real data from garbage. */
    public static final float SENTINEL = 1337.0F;

    private SodiumShaderSource() {}

    static final String GLOBALS_MARKER = "#define CHUNKANIM_GLOBALS 1\n";

    /** Replaces the last member of Sodium's {@code u_Globals} block. */
    static final String GLOBALS_MEMBERS = """
            bool u_UseRGSS;

                // Chunk Animator: x = duration (ms), y = mode, z = easing, w = sentinel
                vec4 u_ChunkAnimParams;
                // Chunk Animator: x = camera Y, y = horizon height, z = level max Y, w = level min Y
                vec4 u_ChunkAnimLevel;""";

    /** Uses Sodium's uniform names; Iris renames some of them, see {@link #irisOffsetFunctions}. */
    static final String OFFSET_FUNCTION = offsetFunctions("u_CurrentTime", "u_RegionID");

    private static final String OFFSET_FUNCTION_TEMPLATE = """
            #ifndef CHUNKANIM_SODIUM
            #define CHUNKANIM_SODIUM
            // Chunk Animator: easeOut functions on normalised time (0..1).
            float chunkanim_ease(int easing, float t) {
                const float PI = 3.14159265358979;
                if (easing == 0) return t;
                if (easing == 1) return -t * (t - 2.0);
                if (easing == 2) { float u = t - 1.0; return u * u * u + 1.0; }
                if (easing == 3) { float u = t - 1.0; return 1.0 - u * u * u * u; }
                if (easing == 4) { float u = t - 1.0; return u * u * u * u * u + 1.0; }
                if (easing == 5) return t >= 1.0 ? 1.0 : 1.0 - exp2(-10.0 * t);
                if (easing == 6) return sin(t * (PI / 2.0));
                if (easing == 7) { float u = t - 1.0; return sqrt(max(0.0, 1.0 - u * u)); }
                if (easing == 8) { float s = 1.70158; float u = t - 1.0; return u * u * ((s + 1.0) * u + s) + 1.0; }
                if (easing == 9) {
                    if (t < 1.0 / 2.75) return 7.5625 * t * t;
                    if (t < 2.0 / 2.75) { float u = t - 1.5 / 2.75; return 7.5625 * u * u + 0.75; }
                    if (t < 2.5 / 2.75) { float u = t - 2.25 / 2.75; return 7.5625 * u * u + 0.9375; }
                    float u = t - 2.625 / 2.75;
                    return 7.5625 * u * u + 0.984375;
                }
                if (easing == 10) {
                    if (t <= 0.0) return 0.0;
                    if (t >= 1.0) return 1.0;
                    return exp2(-10.0 * t) * sin((t - 0.075) * (2.0 * PI) / 0.3) + 1.0;
                }
                return t;
            }

            // Chunk Animator: offset of the section whose origin (relative to the camera) is given.
            vec3 chunkanim_offset(vec3 sectionOrigin) {
            #ifdef CHUNKANIM_GLOBALS
                if (u_ChunkAnimParams.w != 1337.0 || u_ChunkAnimParams.x <= 0.0) {
                    return vec3(0.0);
                }
                int sectionTime = texelFetch(u_SectionTimeInfo, int(($REGION_ID$ * 256u) + _draw_id)).r;
                if (sectionTime < 0) {
                    return vec3(0.0);
                }
                float duration = u_ChunkAnimParams.x;
                float elapsed = float($CURRENT_TIME$ - sectionTime);
                if (elapsed >= duration) {
                    return vec3(0.0);
                }
                float progress = chunkanim_ease(int(u_ChunkAnimParams.z + 0.5), clamp(elapsed / duration, 0.0, 1.0));
                int mode = int(u_ChunkAnimParams.y + 0.5);
                float originY = floor(u_ChunkAnimLevel.x + sectionOrigin.y + 0.5);

                if (mode == 2) {
                    mode = originY < u_ChunkAnimLevel.y ? 0 : 1;
                }
                if (mode == 0) {
                    float distance = abs(originY);
                    return vec3(0.0, distance * progress - distance, 0.0);
                }
                if (mode == 1) {
                    float distance = u_ChunkAnimLevel.z - abs(originY);
                    return vec3(0.0, distance - distance * progress, 0.0);
                }

                // Horizontal slide: move in from beyond the chunk, along its dominant axis from the camera.
                vec2 dif = -(sectionOrigin.xz + 8.0);
                vec2 normal = abs(dif.x) > abs(dif.y)
                        ? vec2(dif.x > 0.0 ? 1.0 : -1.0, 0.0)
                        : vec2(0.0, dif.y > 0.0 ? 1.0 : -1.0);
                float slide = -(200.0 - 200.0 * progress);
                return vec3(normal.x * slide, 0.0, normal.y * slide);
            #else
                return vec3(0.0);
            #endif
            }
            #endif
            """;

    private static String offsetFunctions(final String currentTime, final String regionId) {
        return OFFSET_FUNCTION_TEMPLATE.replace("$CURRENT_TIME$", currentTime).replace("$REGION_ID$", regionId);
    }

    /**
     * Functions for a shader-pack terrain shader generated by Iris. Iris has no per-section
     * origin variable, so the origin is rebuilt from the draw id the same way Sodium does.
     */
    static String irisOffsetFunctions(final String currentTime, final String regionId, final String regionOffset) {
        return offsetFunctions(currentTime, regionId) + """
                #ifndef CHUNKANIM_IRIS
                #define CHUNKANIM_IRIS
                vec3 chunkanim_iris_offset(uint drawId) {
                    vec3 sectionOrigin = %s + vec3((uvec3(drawId) >> uvec3(5u, 0u, 2u)) & uvec3(7u, 3u, 7u)) * 16.0;
                    return chunkanim_offset(sectionOrigin);
                }
                #endif
                """.formatted(regionOffset);
    }

}
