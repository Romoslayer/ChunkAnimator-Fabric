package lumien.chunkanimator.forge;

import lumien.chunkanimator.ChunkAnimator;
import lumien.chunkanimator.ChunkAnimatorCommand;
import lumien.chunkanimator.config.ChunkAnimatorConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge entrypoint. Chunk Animator does nothing on a dedicated server. */
@Mod(ChunkAnimator.MOD_ID)
public final class ChunkAnimatorForge {

    public ChunkAnimatorForge(final FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        ChunkAnimator.init();
        // "Config" button in Forge's mods list.
        context.getContainer().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ChunkAnimatorConfigScreen(parent)));
        RegisterClientCommandsEvent.BUS.addListener(event ->
                event.getDispatcher().register(ChunkAnimatorCommand.<CommandSourceStack>build(
                        (source, message) -> source.sendSuccess(() -> message, false),
                        CommandSourceStack::sendFailure)));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> ChunkAnimator.onClientTick(Minecraft.getInstance()));
    }

}
