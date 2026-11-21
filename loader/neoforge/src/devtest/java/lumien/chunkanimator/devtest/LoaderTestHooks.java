package lumien.chunkanimator.devtest;

import lumien.chunkanimator.ChunkAnimator;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

final class LoaderTestHooks {

    private LoaderTestHooks() {}

    /** The screen NeoForge's mods list opens for Chunk Animator's "Config" button, or null. */
    static Screen configScreenFromModList(final Screen parent) {
        final ModContainer container = ModList.get().getModContainerById(ChunkAnimator.MOD_ID).orElseThrow();
        return IConfigScreenFactory.getForMod(container.getModInfo())
                .map(factory -> factory.createScreen(container, parent))
                .orElse(null);
    }

}
