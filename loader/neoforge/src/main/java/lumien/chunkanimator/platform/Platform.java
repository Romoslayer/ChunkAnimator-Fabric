package lumien.chunkanimator.platform;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;

import java.nio.file.Path;

/** NeoForge implementation of the small loader-specific surface the shared code needs. */
public final class Platform {

    private Platform() {}

    public static String loaderName() {
        return "NeoForge";
    }

    public static boolean isModLoaded(final String modId) {
        final ModList mods = ModList.get();
        return mods != null ? mods.isLoaded(modId) : isModLoadingEarly(modId);
    }

    /** Works while mixin configs are loading, before {@link ModList} exists. */
    public static boolean isModLoadingEarly(final String modId) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
    }

    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

}
