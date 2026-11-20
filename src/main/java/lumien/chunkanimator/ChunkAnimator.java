package lumien.chunkanimator;

import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.config.ChunkAnimatorConfigScreen;
import lumien.chunkanimator.handler.AnimationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. Chunk Animator is purely client-side: it only changes where chunk sections
 * are drawn, never the world itself, so it works on any server.
 *
 * @author lumien231
 */
public final class ChunkAnimator implements ClientModInitializer {

    public static final String MOD_ID = "chunkanimator";
    public static final Logger LOGGER = LoggerFactory.getLogger("Chunk Animator");

    @Override
    public void onInitializeClient() {
        ChunkAnimatorConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ChunkAnimatorCommand.register(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openConfigScreen) {
                openConfigScreen = false;
                client.gui.setScreen(new ChunkAnimatorConfigScreen(client.gui.screen()));
            }
        });

        LOGGER.info("Chunk Animator loaded ({} renderer)", isSodiumLoaded() ? "Sodium" : "vanilla");
    }

    private static volatile boolean openConfigScreen;

    /** Opens the settings screen on the next client tick. */
    public static void openConfigScreenNextTick() {
        openConfigScreen = true;
    }

    public static boolean isSodiumLoaded() {
        return FabricLoader.getInstance().isModLoaded("sodium");
    }

    /** The vanilla-renderer animation handler, created on first use (render thread). */
    public static AnimationHandler handler() {
        return HandlerHolder.INSTANCE;
    }

    private static final class HandlerHolder {
        private static final AnimationHandler INSTANCE = new AnimationHandler();
    }

}
