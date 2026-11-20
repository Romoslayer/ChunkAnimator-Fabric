package lumien.chunkanimator.shader;

import lumien.chunkanimator.ChunkAnimator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies small, targeted text patches to the terrain shaders while they are loaded from
 * resources. Every patch is optional: if a resource pack or another mod has changed a shader so
 * that a patch no longer matches, that shader is left untouched and the mod falls back to a
 * safe mode instead of breaking rendering.
 *
 * <ul>
 *     <li><b>Vanilla</b> ({@code minecraft:core/terrain}): vanilla only sends whole-block section
 *     positions to the GPU. To keep animations smooth, the sub-block part of the offset is packed
 *     into the section's {@code ChunkVisibility} value; the patch decodes it again.</li>
 *     <li><b>Sodium</b> ({@code sodium:blocks/block_layer_opaque}): Sodium draws whole regions per
 *     draw call, so per-section offsets are computed on the GPU from Sodium's own per-section
 *     "first built" timestamps, plus two vec4s of settings appended to Sodium's globals block.</li>
 * </ul>
 */
public final class ShaderPatcher {

    private static final Pattern VOID_MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*\\)");

    // --- vanilla -----------------------------------------------------------------------------

    private static final Pattern VANILLA_POSITION = Pattern.compile(
            "vec3\\s+pos\\s*=\\s*Position\\s*\\+\\s*\\(\\s*ChunkPosition\\s*-\\s*CameraBlockPos\\s*\\)\\s*\\+\\s*CameraOffset\\s*;");
    private static final Pattern VANILLA_VISIBILITY_MIX = Pattern.compile("mix\\(\\s*1\\.0\\s*,\\s*ChunkVisibility\\s*,");
    private static final Pattern VANILLA_FRAGMENT_VISIBILITY = Pattern.compile("color\\s*,\\s*ChunkVisibility\\s*\\)");
    private static final Pattern VISIBILITY_DECLARATION = Pattern.compile("float\\s+ChunkVisibility\\s*;");
    private static final Pattern VISIBILITY_TOKEN = Pattern.compile("\\bChunkVisibility\\b");
    private static final String DECODE_CALL = "chunkanim_decode(ChunkVisibility)";

    private static volatile boolean vanillaVertexPatched;
    private static volatile boolean vanillaFragmentPatched;

    // --- sodium ------------------------------------------------------------------------------

    private static final Pattern SODIUM_GLOBALS_BLOCK = Pattern.compile("layout\\s*\\(\\s*std140\\s*\\)\\s*uniform\\s+u_Globals\\b");
    private static final Pattern SODIUM_GLOBALS_LAST_MEMBER = Pattern.compile("bool\\s+u_UseRGSS\\s*;");
    private static final Pattern SODIUM_POSITION = Pattern.compile("vec3\\s+position\\s*=\\s*_vert_position\\s*\\+\\s*translation\\s*;");

    private static volatile boolean sodiumGlobalsPatched;
    private static volatile boolean sodiumVertexPatched;

    private ShaderPatcher() {}

    /**
     * @return true if the vanilla terrain shaders understand packed visibility values
     */
    public static boolean isVanillaTerrainPatched() {
        return vanillaVertexPatched && vanillaFragmentPatched;
    }

    public static boolean isSodiumPatched() {
        return sodiumGlobalsPatched && sodiumVertexPatched;
    }

    /** @return true once a shader pack's terrain shaders have been patched through Iris */
    public static boolean isIrisPatched() {
        return irisLogged;
    }

    /**
     * Packs a sub-block offset into a visibility value. Plain visibility values are always in
     * {@code [0, 1]}, so anything above 1.5 can be recognised as packed by the shader.
     *
     * @param visibility vanilla fade-in visibility, 0..1
     * @param axis       0 = x, 1 = y, 2 = z
     * @param fraction   sub-block offset in 1/256ths of a block, 0..255
     */
    public static float packVisibility(float visibility, int axis, int fraction) {
        final float clamped = Math.max(0F, Math.min(1F, visibility));
        return clamped + 2F * (1 + axis * 256 + fraction);
    }

    /**
     * Patches a shader source if it is one this mod cares about.
     *
     * @param namespace the resource namespace, e.g. {@code minecraft}
     * @param path      the resource path, e.g. {@code shaders/core/terrain.vsh} or {@code globals.glsl}
     * @param source    the shader source
     * @return the (possibly) patched source
     */
    public static String patch(final String namespace, final String path, final String source) {
        if (source == null) {
            return null;
        }
        try {
            if ("minecraft".equals(namespace)) {
                if (path.endsWith("core/terrain.vsh")) {
                    return patchVanillaVertex(source);
                }
                if (path.endsWith("core/terrain.fsh")) {
                    return patchVanillaFragment(source);
                }
                return source;
            }
            if ("sodium".equals(namespace)) {
                String patched = source;
                if (SODIUM_GLOBALS_LAST_MEMBER.matcher(patched).find()) {
                    patched = patchSodiumGlobals(patched);
                }
                if (path.endsWith("blocks/block_layer_opaque.vsh")) {
                    patched = patchSodiumVertex(patched);
                }
                return patched;
            }
        } catch (RuntimeException e) {
            ChunkAnimator.LOGGER.error("Failed to patch shader {}:{}, leaving it untouched", namespace, path, e);
        }
        return source;
    }

    private static String patchVanillaVertex(final String source) {
        final Matcher position = VANILLA_POSITION.matcher(source);
        if (!position.find()) {
            vanillaVertexPatched = false;
            ChunkAnimator.LOGGER.warn("Could not find the section position in minecraft:core/terrain.vsh (replaced by a resource pack?). "
                    + "Chunk animations will move in whole-block steps.");
            return source;
        }

        String patched = position.replaceFirst(Matcher.quoteReplacement(
                "vec4 chunkanim_data = " + DECODE_CALL + ";\n"
                + "    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset + chunkanim_data.xyz;"));
        patched = VANILLA_VISIBILITY_MIX.matcher(patched).replaceAll(Matcher.quoteReplacement("mix(1.0, chunkanim_data.w,"));
        patched = insertBeforeMain(patched, VanillaShaderSource.DECODE_FUNCTION);

        if (patched == null || hasRawVisibilityUsage(patched)) {
            vanillaVertexPatched = false;
            ChunkAnimator.LOGGER.warn("minecraft:core/terrain.vsh uses ChunkVisibility in an unexpected way; not patching it. "
                    + "Chunk animations will move in whole-block steps.");
            return source;
        }

        vanillaVertexPatched = true;
        return patched;
    }

    private static String patchVanillaFragment(final String source) {
        if (!hasRawVisibilityUsage(source)) {
            // Modern terrain.fsh gets the visibility from the vertex shader; nothing to do.
            vanillaFragmentPatched = true;
            return source;
        }

        String patched = VANILLA_FRAGMENT_VISIBILITY.matcher(source)
                .replaceAll(Matcher.quoteReplacement("color, " + DECODE_CALL + ".w)"));
        patched = insertBeforeMain(patched, VanillaShaderSource.DECODE_FUNCTION);

        if (patched == null || hasRawVisibilityUsage(patched)) {
            vanillaFragmentPatched = false;
            ChunkAnimator.LOGGER.warn("minecraft:core/terrain.fsh uses ChunkVisibility in an unexpected way; not patching it. "
                    + "Chunk animations will move in whole-block steps.");
            return source;
        }

        vanillaFragmentPatched = true;
        return patched;
    }

    /**
     * Checks whether the raw {@code ChunkVisibility} value is still read anywhere other than its
     * declaration or our decode call. If it is, sending packed values would break that code.
     */
    private static boolean hasRawVisibilityUsage(final String source) {
        String stripped = VISIBILITY_DECLARATION.matcher(source).replaceAll("");
        stripped = stripped.replace(DECODE_CALL, "");
        return VISIBILITY_TOKEN.matcher(stripped).find();
    }

    private static String patchSodiumGlobals(final String source) {
        final Matcher block = SODIUM_GLOBALS_BLOCK.matcher(source);
        if (!block.find()) {
            sodiumGlobalsPatched = false;
            return source;
        }
        String patched = source.substring(0, block.start()) + SodiumShaderSource.GLOBALS_MARKER + source.substring(block.start());
        patched = SODIUM_GLOBALS_LAST_MEMBER.matcher(patched).replaceFirst(Matcher.quoteReplacement(SodiumShaderSource.GLOBALS_MEMBERS));
        sodiumGlobalsPatched = true;
        return patched;
    }

    private static String patchSodiumVertex(final String source) {
        final Matcher position = SODIUM_POSITION.matcher(source);
        if (!position.find()) {
            sodiumVertexPatched = false;
            ChunkAnimator.LOGGER.warn("Could not find the vertex position in Sodium's terrain shader; chunk animations are disabled "
                    + "while Sodium is rendering. This Sodium version may not be supported yet.");
            return source;
        }
        final String withOffset = position.replaceFirst(Matcher.quoteReplacement(
                position.group() + "\n    position += chunkanim_offset(translation);"));
        final String patched = insertBeforeMain(withOffset, SodiumShaderSource.OFFSET_FUNCTION);
        if (patched == null) {
            sodiumVertexPatched = false;
            return source;
        }
        sodiumVertexPatched = true;
        ChunkAnimator.LOGGER.info("Patched Sodium terrain shader for chunk animations");
        return patched;
    }

    // --- iris ----------------------------------------------------------------------------------

    private static final Pattern IRIS_VERT_INIT = Pattern.compile("void\\s+_vert_init\\s*\\(\\s*\\)\\s*\\{");
    private static final Pattern IRIS_DRAW_ID = Pattern.compile("_draw_id\\s*=\\s*a_LightAndData\\s*\\[\\s*3u?\\s*\\]\\s*;");

    private static volatile boolean irisWarned;
    private static volatile boolean irisLogged;

    /**
     * Patches the shaders Iris generates from a shader pack for Sodium's terrain passes
     * (including the shadow pass). Iris declares Sodium's {@code u_Globals} block itself and
     * decodes vertices in its own {@code _vert_init()}; the animation offset is added to
     * {@code _vert_position} there, so it applies whether the pack reads {@code gl_Vertex} or
     * {@code vaPosition}.
     *
     * @param sources Iris's stage -> source map (keys are Iris's {@code PatchShaderType})
     * @return a patched copy, or the original map if the shaders don't look as expected
     */
    public static <K> Map<K, String> patchIrisSodium(final Map<K, String> sources) {
        if (sources == null) {
            return null;
        }
        try {
            K vertexKey = null;
            for (K key : sources.keySet()) {
                if ("VERTEX".equals(String.valueOf(key))) {
                    vertexKey = key;
                }
            }
            final String vertex = vertexKey == null ? null : sources.get(vertexKey);
            if (vertex == null) {
                return sources;
            }

            final Matcher vertInit = IRIS_VERT_INIT.matcher(vertex);
            final Matcher drawId = IRIS_DRAW_ID.matcher(vertex);
            if (!vertInit.find() || !drawId.find() || !SODIUM_GLOBALS_LAST_MEMBER.matcher(vertex).find()
                    || !vertex.contains("u_SectionTimeInfo")) {
                if (!irisWarned) {
                    irisWarned = true;
                    ChunkAnimator.LOGGER.warn("This Iris version generates terrain shaders Chunk Animator doesn't recognise; "
                            + "chunks will appear without animation while a shader pack is active.");
                }
                return sources;
            }

            final String currentTime = vertex.contains("iris_CurrentTime") ? "iris_CurrentTime" : "u_CurrentTime";
            final String regionId = vertex.contains("iris_RegionID") ? "iris_RegionID" : "u_RegionID";
            final String regionOffset = vertex.contains("iris_RegionOffset") ? "iris_RegionOffset" : "u_RegionOffset";

            // Prototype before _vert_init (the definition is appended at the end, after every uniform).
            String patched = vertex.substring(0, vertInit.start()) + "vec3 chunkanim_iris_offset(uint drawId);\n"
                    + vertex.substring(vertInit.start());
            final Matcher drawIdAfter = IRIS_DRAW_ID.matcher(patched);
            drawIdAfter.find();
            patched = patched.substring(0, drawIdAfter.end()) + " _vert_position += chunkanim_iris_offset(_draw_id);"
                    + patched.substring(drawIdAfter.end());
            patched = patchSodiumGlobals(patched);
            patched = patched + "\n" + SodiumShaderSource.irisOffsetFunctions(currentTime, regionId, regionOffset);

            final Map<K, String> result = new LinkedHashMap<>(sources);
            for (Map.Entry<K, String> entry : result.entrySet()) {
                if (entry.getKey() == vertexKey) {
                    entry.setValue(patched);
                } else if (entry.getValue() != null && SODIUM_GLOBALS_LAST_MEMBER.matcher(entry.getValue()).find()) {
                    // Every stage must declare the uniform block identically.
                    entry.setValue(patchSodiumGlobals(entry.getValue()));
                }
            }

            if (!irisLogged) {
                irisLogged = true;
                ChunkAnimator.LOGGER.info("Patched Iris shader pack terrain shaders for chunk animations");
            }
            return result;
        } catch (RuntimeException e) {
            ChunkAnimator.LOGGER.error("Failed to patch an Iris terrain shader, leaving it untouched", e);
            return sources;
        }
    }

    /** @return the source with {@code code} inserted before {@code void main()}, or null if there is no main */
    private static String insertBeforeMain(final String source, final String code) {
        final Matcher main = VOID_MAIN.matcher(source);
        if (!main.find()) {
            return null;
        }
        return source.substring(0, main.start()) + code + "\n" + source.substring(main.start());
    }

}
