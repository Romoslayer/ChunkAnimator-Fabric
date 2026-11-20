package lumien.chunkanimator.compat;

import com.mojang.blaze3d.buffers.Std140Builder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.shader.SodiumShaderSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Arrays;

/**
 * Java side of the Sodium integration. Only ever loaded when Sodium is installed.
 *
 * <p>Sodium already records, per chunk section, the time its first mesh was uploaded (it uses
 * this for its own fade-in). The patched Sodium shader reads that timestamp and computes the
 * animation on the GPU, so no per-section work is needed on the CPU each frame. This class just
 * supplies the settings, and mirrors the timestamps so block-face culling can be relaxed for
 * sections that are mid-animation (they are drawn somewhere other than where Sodium thinks).</p>
 */
public final class SodiumCompat {

    /** Sections per Sodium render region (8 x 4 x 8). */
    private static final int REGION_SIZE = 256;
    private static final int DISABLE_AROUND_PLAYER_DISTANCE_SQR = 64 * 64;

    /** region id -> first-upload time of each section, relative to the region's creation time. Render thread only. */
    private static final Int2ObjectOpenHashMap<int[]> SECTION_TIMES = new Int2ObjectOpenHashMap<>();

    private SodiumCompat() {}

    /** Appends {@code u_ChunkAnimParams} and {@code u_ChunkAnimLevel} to Sodium's globals block. */
    public static Std140Builder appendGlobals(final Std140Builder builder) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;

        float cameraY = 0F;
        float horizon = 63F;
        float maxY = 320F;
        float minY = -64F;
        if (level != null) {
            cameraY = (float) mc.gameRenderer.mainCamera().position().y;
            horizon = (float) level.getLevelData().getHorizonHeight(level);
            minY = level.getMinY();
            maxY = level.getMinY() + level.getHeight();
        }

        return builder
                .putVec4(ChunkAnimatorConfig.animationDuration(), ChunkAnimatorConfig.mode().ordinal(),
                        ChunkAnimatorConfig.easingFunction().ordinal(), SodiumShaderSource.SENTINEL)
                .putVec4(cameraY, horizon, maxY, minY);
    }

    /**
     * Replacement for Sodium's "is this section close to the player" fade threshold.
     * Sodium never fades sections within ~28 blocks; the original Chunk Animator animates
     * everything unless {@code disableAroundPlayer} is set, in which case the radius is 64.
     */
    public static double nearbyDistanceSquared() {
        return ChunkAnimatorConfig.disableAroundPlayer() ? DISABLE_AROUND_PLAYER_DISTANCE_SQR : -1.0;
    }

    public static void recordSectionTime(final int regionId, final int sectionIndex, final int relativeTime) {
        if (sectionIndex < 0 || sectionIndex >= REGION_SIZE) {
            return;
        }
        int[] times = SECTION_TIMES.get(regionId);
        if (times == null) {
            times = new int[REGION_SIZE];
            Arrays.fill(times, -1);
            SECTION_TIMES.put(regionId, times);
        }
        times[sectionIndex] = relativeTime;
    }

    public static void clearRegion(final int regionId) {
        SECTION_TIMES.remove(regionId);
    }

    public static void clearAll() {
        SECTION_TIMES.clear();
    }

    /**
     * @return true if the given section is currently being animated by the shader
     */
    public static boolean isAnimating(final int regionId, final long regionCreationTime, final int sectionIndex) {
        final int duration = ChunkAnimatorConfig.animationDuration();
        if (duration <= 0 || sectionIndex < 0 || sectionIndex >= REGION_SIZE) {
            return false;
        }
        final int[] times = SECTION_TIMES.get(regionId);
        if (times == null || times[sectionIndex] < 0) {
            return false;
        }
        final long elapsed = (System.currentTimeMillis() - regionCreationTime) - times[sectionIndex];
        return elapsed < duration;
    }

}
