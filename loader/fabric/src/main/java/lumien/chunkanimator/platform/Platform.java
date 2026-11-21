package lumien.chunkanimator.platform;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Fabric implementation of the small loader-specific surface the shared code needs. */
public final class Platform {

    private Platform() {}

    public static String loaderName() {
        return "Fabric";
    }

    public static boolean isModLoaded(final String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    /** Same as {@link #isModLoaded}, but safe to call while mixin configs are loading. */
    public static boolean isModLoadingEarly(final String modId) {
        return isModLoaded(modId);
    }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

}
