package lumien.chunkanimator.neoforge;

import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.ChunkAnimatorCommand;
import lumien.chunkanimator.config.ChunkAnimatorConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** NeoForge entrypoint. Only constructed on the client. */
@Mod(value = ChunkAnimator.MOD_ID, dist = Dist.CLIENT)
public final class ChunkAnimatorNeoForge {

    public ChunkAnimatorNeoForge(final IEventBus modBus, final ModContainer container) {
        ChunkAnimator.init();
        // "Config" button in NeoForge's mods list.
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ChunkAnimatorConfigScreen(parent));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event ->
                event.getDispatcher().register(ChunkAnimatorCommand.<CommandSourceStack>build(
                        (source, message) -> source.sendSuccess(() -> message, false),
                        CommandSourceStack::sendFailure)));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ChunkAnimator.onClientTick(Minecraft.getInstance()));
    }

}
