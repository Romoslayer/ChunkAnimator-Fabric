package lumien.chunkanimator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Client configuration, stored as {@code config/chunkanimator.json}.
 *
 * <p>Values are read on the render thread every frame, so they are kept in plain volatile
 * fields rather than behind any lookup.</p>
 *
 * @author lumien231
 */
public final class ChunkAnimatorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Platform.configDir().resolve(ChunkAnimator.MOD_ID + ".json");

    public static final AnimationMode DEFAULT_MODE = AnimationMode.BELOW;
    public static final EasingFunction DEFAULT_EASING = EasingFunction.SINE;
    public static final int DEFAULT_DURATION = 1000;
    public static final int MAX_DURATION = 60_000;

    /** The animation mode - controls how the chunks should be animated. */
    private static volatile AnimationMode mode = DEFAULT_MODE;
    /** The easing function - controls which easing function should be used. */
    private static volatile EasingFunction easingFunction = DEFAULT_EASING;
    /** The animation duration - controls how long the animation should last (in milliseconds). */
    private static volatile int animationDuration = DEFAULT_DURATION;
    /** Disable around player - disables animation of chunks near the player. */
    private static volatile boolean disableAroundPlayer = false;

    private ChunkAnimatorConfig() {}

    public static AnimationMode mode() { return mode; }
    public static EasingFunction easingFunction() { return easingFunction; }
    public static int animationDuration() { return animationDuration; }
    public static boolean disableAroundPlayer() { return disableAroundPlayer; }

    public static void setMode(AnimationMode value) { mode = value; }
    public static void setEasingFunction(EasingFunction value) { easingFunction = value; }
    public static void setAnimationDuration(int value) { animationDuration = clampDuration(value); }
    public static void setDisableAroundPlayer(boolean value) { disableAroundPlayer = value; }

    private static int clampDuration(int value) {
        return Math.max(0, Math.min(MAX_DURATION, value));
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }

        try {
            final JsonObject json = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            mode = readEnum(json, "mode", AnimationMode.class, DEFAULT_MODE);
            easingFunction = readEnum(json, "easingFunction", EasingFunction.class, DEFAULT_EASING);
            animationDuration = json.has("animationDuration")
                    ? clampDuration(json.get("animationDuration").getAsInt()) : DEFAULT_DURATION;
            disableAroundPlayer = json.has("disableAroundPlayer") && json.get("disableAroundPlayer").getAsBoolean();
        } catch (Exception e) {
            ChunkAnimator.LOGGER.error("Failed to read {}, using defaults", PATH, e);
        }

        // Write back so newly added or corrected values show up in the file.
        save();
    }

    public static void save() {
        final JsonObject json = new JsonObject();
        json.addProperty("_comment_mode", "BELOW, ABOVE, HYBRID (below if under the horizon, else above), "
                + "HORIZONTAL_SLIDE, HORIZONTAL_SLIDE_ALTERNATE");
        json.addProperty("mode", mode.name());
        json.addProperty("_comment_easingFunction", "LINEAR, QUAD, CUBIC, QUART, QUINT, EXPO, SINE, CIRC, BACK, BOUNCE, ELASTIC");
        json.addProperty("easingFunction", easingFunction.name());
        json.addProperty("_comment_animationDuration", "How long the animation lasts, in milliseconds (0 disables it).");
        json.addProperty("animationDuration", animationDuration);
        json.addProperty("_comment_disableAroundPlayer", "If true, chunks next to the player will not animate.");
        json.addProperty("disableAroundPlayer", disableAroundPlayer);

        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ChunkAnimator.LOGGER.error("Failed to write {}", PATH, e);
        }
    }

    private static <E extends Enum<E>> E readEnum(JsonObject json, String key, Class<E> type, E fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, json.get(key).getAsString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ChunkAnimator.LOGGER.warn("Unknown value {} for {} in {}, using {}", json.get(key), key, PATH, fallback);
            return fallback;
        }
    }
}
