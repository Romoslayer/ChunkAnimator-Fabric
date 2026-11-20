package lumien.chunkanimator.handler;

import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.shader.ShaderPatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

/**
 * Drives the animation for Minecraft's own chunk renderer.
 *
 * <p>Vanilla uploads one small record per visible chunk section every frame (the section's
 * integer position plus its fade-in "visibility"). This handler rewrites that record while a
 * section is animating: the whole-block part of the offset is added to the section position and,
 * when the terrain shader has been patched by {@link ShaderPatcher}, the sub-block remainder is
 * packed into the visibility value so the movement stays perfectly smooth.</p>
 *
 * <p>Everything here runs on the render thread and allocates nothing per call.</p>
 *
 * @author lumien231
 */
public final class AnimationHandler {

    /** Fixed-point precision used for the sub-block part of the offset (1/256th of a block). */
    private static final int FRACTION_STEPS = 256;
    private static final int DISABLE_AROUND_PLAYER_DISTANCE_SQR = 64 * 64;

    private final Minecraft mc = Minecraft.getInstance();
    private final float[] offset = new float[3];
    private final int[] wholeOffset = new int[3];
    private final SectionTransform transform = new SectionTransform();

    /**
     * Applies the animation to a section that is about to be drawn.
     *
     * @return the transformed values, or {@code null} if the section is not animating and the
     * vanilla values should be used as-is
     */
    public SectionTransform apply(final AnimatedSection section, final int x, final int y, final int z, final float visibility) {
        long start = section.chunkanimator$getAnimationStart();
        if (start == AnimatedSection.DONE) {
            return null;
        }

        final ClientLevel level = this.mc.level;
        if (level == null) {
            return null;
        }

        final int duration = ChunkAnimatorConfig.animationDuration();
        final AnimationMode mode = ChunkAnimatorConfig.mode();
        final long now = Util.getMillis();

        if (start == AnimatedSection.NOT_STARTED) {
            // First time this section is drawn with geometry: decide whether it should animate.
            if (duration <= 0 || section.chunkanimator$shouldSkip() || !this.shouldAnimate(x, z)) {
                section.chunkanimator$setAnimationStart(AnimatedSection.DONE);
                return null;
            }
            start = Math.max(1L, now);
            section.chunkanimator$setAnimationStart(start);
            section.chunkanimator$setFacing(mode.isHorizontal() ? this.facingFromPlayer(x, z) : null);
        }

        final long elapsed = now - start;
        if (elapsed >= duration) {
            section.chunkanimator$setAnimationStart(AnimatedSection.DONE);
            return null;
        }

        Direction facing = section.chunkanimator$getFacing();
        if (facing == null && mode.isHorizontal()) {
            // The mode was switched to a horizontal one mid-animation.
            facing = this.facingFromPlayer(x, z);
            section.chunkanimator$setFacing(facing);
        }

        AnimationMath.computeOffset(mode, ChunkAnimatorConfig.easingFunction(), Math.max(0L, elapsed), duration, y, facing,
                level.getLevelData().getHorizonHeight(level), level.getMinY() + level.getHeight(), this.offset);

        return this.encode(x, y, z, visibility);
    }

    private SectionTransform encode(final int x, final int y, final int z, final float visibility) {
        final SectionTransform out = this.transform;
        out.visibility = visibility;

        if (!ShaderPatcher.isVanillaTerrainPatched()) {
            // The terrain shader could not be patched (e.g. a resource pack replaced it), so only
            // whole-block offsets are possible. Still animates, just in one-block steps.
            out.x = x + Math.round(this.offset[0]);
            out.y = y + Math.round(this.offset[1]);
            out.z = z + Math.round(this.offset[2]);
            return out;
        }

        int packedAxis = -1;
        int packedFraction = 0;
        final int[] whole = this.wholeOffset;
        for (int axis = 0; axis < 3; axis++) {
            final long fixed = Math.round((double) this.offset[axis] * FRACTION_STEPS);
            whole[axis] = (int) Math.floorDiv(fixed, FRACTION_STEPS);
            final int fraction = (int) Math.floorMod(fixed, FRACTION_STEPS);
            if (fraction != 0 && packedAxis == -1) {
                packedAxis = axis;
                packedFraction = fraction;
            }
        }

        out.x = x + whole[0];
        out.y = y + whole[1];
        out.z = z + whole[2];

        if (packedAxis != -1) {
            out.visibility = ShaderPatcher.packVisibility(visibility, packedAxis, packedFraction);
        }
        return out;
    }

    private boolean shouldAnimate(final int sectionX, final int sectionZ) {
        if (!ChunkAnimatorConfig.disableAroundPlayer()) {
            return true;
        }
        final LocalPlayer player = this.mc.player;
        if (player == null) {
            return true;
        }
        final long difX = player.getBlockX() - (sectionX + 8L);
        final long difZ = player.getBlockZ() - (sectionZ + 8L);
        return difX * difX + difZ * difZ > DISABLE_AROUND_PLAYER_DISTANCE_SQR;
    }

    private Direction facingFromPlayer(final int sectionX, final int sectionZ) {
        final LocalPlayer player = this.mc.player;
        if (player == null) {
            return null;
        }
        return AnimationMath.getChunkFacing(player.getBlockX() - (sectionX + 8), player.getBlockZ() - (sectionZ + 8));
    }

    /** Mutable result holder, re-used for every call. */
    public static final class SectionTransform {
        public int x;
        public int y;
        public int z;
        public float visibility;
    }

}
