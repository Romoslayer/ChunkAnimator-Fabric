package lumien.chunkanimator;

import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.config.ChunkAnimatorConfigScreen;
import lumien.chunkanimator.handler.AnimationHandler;
import lumien.chunkanimator.platform.Platform;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Loader-independent core. Each mod loader has a small entrypoint (in {@code loader/<name>}) that
 * calls {@link #init()}, registers {@link ChunkAnimatorCommand} and calls {@link #onClientTick(Minecraft)}.
 *
 * <p>Chunk Animator is purely client-side: it only changes where chunk sections are drawn, never
 * the world itself, so it works on any server.</p>
 *
 * @author lumien231
 */
public final class ChunkAnimator {

    public static final String MOD_ID = "chunkanimator";
    public static final Logger LOGGER = LoggerFactory.getLogger("Chunk Animator");

    private static volatile boolean openConfigScreen;
    /** Development-only in-game test (see loader/devtest). Always null in normal play. */
    private static Consumer<Minecraft> devTest;

    private ChunkAnimator() {}

    public static void init() {
        ChunkAnimatorConfig.load();
        LOGGER.info("Chunk Animator loaded on {} ({} renderer)", Platform.loaderName(), isSodiumLoaded() ? "Sodium" : "vanilla");
        if (Boolean.getBoolean("chunkanimator.smokeTest")) {
            devTest = loadDevTest();
            LOGGER.info("Development smoke test enabled: {}", devTest != null);
        }
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Minecraft> loadDevTest() {
        try {
            return (Consumer<Minecraft>) Class.forName("lumien.chunkanimator.devtest.SmokeTest").getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            LOGGER.error("chunkanimator.smokeTest is set but the smoke test is not on the classpath", e);
            return null;
        }
    }

    /** Called by the loader at the end of every client tick. */
    public static void onClientTick(final Minecraft client) {
        if (openConfigScreen) {
            openConfigScreen = false;
            client.gui.setScreen(new ChunkAnimatorConfigScreen(client.gui.screen()));
        }
        if (devTest != null) {
            devTest.accept(client);
        }
    }

    /** Opens the settings screen on the next client tick. */
    public static void openConfigScreenNextTick() {
        openConfigScreen = true;
    }

    public static boolean isSodiumLoaded() {
        return Platform.isModLoaded("sodium");
    }

    /** The vanilla-renderer animation handler, created on first use (render thread). */
    public static AnimationHandler handler() {
        return HandlerHolder.INSTANCE;
    }

    private static final class HandlerHolder {
        private static final AnimationHandler INSTANCE = new AnimationHandler();
    }

}
