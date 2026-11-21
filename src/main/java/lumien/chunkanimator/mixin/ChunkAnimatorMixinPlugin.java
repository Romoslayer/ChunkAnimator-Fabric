package lumien.chunkanimator.mixin;

import lumien.chunkanimator.platform.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Picks which renderer hooks to apply:
 * <ul>
 *     <li>{@code mixin.common}: shader patching, always applied.</li>
 *     <li>{@code mixin.sodium}: only when Sodium is installed.</li>
 *     <li>{@code mixin.iris}: only when Iris (and therefore Sodium) is installed.</li>
 *     <li>{@code mixin.vanilla}: only when Minecraft's own chunk renderer is in use.</li>
 * </ul>
 */
public final class ChunkAnimatorMixinPlugin implements IMixinConfigPlugin {

    /** Mods that replace the chunk renderer in a way Chunk Animator does not support. */
    private static final String[] OTHER_CHUNK_RENDERERS = {"canvas", "vulkanmod", "embeddium"};

    private boolean sodium;
    private boolean iris;
    private boolean vanillaRenderer;

    @Override
    public void onLoad(String mixinPackage) {
        this.sodium = Platform.isModLoadingEarly("sodium");
        this.iris = this.sodium && Platform.isModLoadingEarly("iris");

        boolean otherRenderer = false;
        for (String id : OTHER_CHUNK_RENDERERS) {
            otherRenderer |= Platform.isModLoadingEarly(id);
        }
        this.vanillaRenderer = !this.sodium && !otherRenderer;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".mixin.iris.")) {
            return this.iris;
        }
        if (mixinClassName.contains(".mixin.sodium.")) {
            return this.sodium;
        }
        if (mixinClassName.contains(".mixin.vanilla.")) {
            return this.vanillaRenderer;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

}
