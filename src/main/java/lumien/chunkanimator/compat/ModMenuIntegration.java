package lumien.chunkanimator.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import lumien.chunkanimator.config.ChunkAnimatorConfigScreen;

/**
 * Adds a "Configure" button for Chunk Animator in Mod Menu. Only loaded when Mod Menu is
 * installed (it is referenced solely from the {@code modmenu} entrypoint).
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChunkAnimatorConfigScreen::new;
    }

}
