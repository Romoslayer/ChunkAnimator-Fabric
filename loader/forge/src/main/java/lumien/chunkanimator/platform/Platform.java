package lumien.chunkanimator.platform;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

import java.nio.file.Path;

/** Forge implementation of the small loader-specific surface the shared code needs. */
public final class Platform {

    private Platform() {}

    public static String loaderName() {
        return "Forge";
    }

    public static boolean isModLoaded(final String modId) {
        return ModList.isLoaded(modId);
    }

    /** Works while mixin configs are loading, before the mod list is built. */
    public static boolean isModLoadingEarly(final String modId) {
        return LoadingModList.getModFileById(modId) != null;
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

}
