package lumien.chunkanimator.fabric;

import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.ChunkAnimatorCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Fabric client entrypoint. */
public final class ChunkAnimatorFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ChunkAnimator.init();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ChunkAnimatorCommand.<FabricClientCommandSource>build(
                        FabricClientCommandSource::sendFeedback, FabricClientCommandSource::sendError)));
        ClientTickEvents.END_CLIENT_TICK.register(ChunkAnimator::onClientTick);
    }

}
