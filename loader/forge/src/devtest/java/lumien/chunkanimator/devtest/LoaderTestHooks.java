package lumien.chunkanimator.devtest;

import lumien.chunkanimator.ChunkAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;

final class LoaderTestHooks {

    private LoaderTestHooks() {}

    /** The screen Forge's mods list opens for Chunk Animator's "Config" button, or null. */
    static Screen configScreenFromModList(final Screen parent) {
        final var info = ModList.getModContainerById(ChunkAnimator.MOD_ID).orElseThrow().getModInfo();
        return ConfigScreenHandler.getScreenFactoryFor(info)
                .map(factory -> factory.apply(Minecraft.getInstance(), parent))
                .orElse(null);
    }

}
