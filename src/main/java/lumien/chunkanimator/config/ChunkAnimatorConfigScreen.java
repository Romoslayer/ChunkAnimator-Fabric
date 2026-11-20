package lumien.chunkanimator.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;

/**
 * In-game settings screen, built from vanilla option widgets so it looks and behaves like
 * Minecraft's own options menus. Changes apply immediately (watch chunks load behind the
 * screen) and are written to {@code config/chunkanimator.json} when the screen closes.
 *
 * <p>Opened from Mod Menu, or with {@code /chunkanimator config}.</p>
 */
public final class ChunkAnimatorConfigScreen extends OptionsSubScreen {

    /** The duration slider moves in steps of this many milliseconds. */
    private static final int DURATION_STEP = 50;
    private static final int DURATION_SLIDER_MAX = 10_000;

    public ChunkAnimatorConfigScreen(final Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("chunkanimator.config.title"));
    }

    @Override
    protected void addOptions() {
        if (this.list == null) {
            return;
        }

        this.list.addBig(enumOption("mode", AnimationMode.class, AnimationMode.values(),
                ChunkAnimatorConfig.mode(), ChunkAnimatorConfig::setMode));
        this.list.addBig(enumOption("easingFunction", EasingFunction.class, EasingFunction.values(),
                ChunkAnimatorConfig.easingFunction(), ChunkAnimatorConfig::setEasingFunction));
        this.list.addBig(durationOption());
        this.list.addBig(OptionInstance.createBoolean(
                "chunkanimator.config.disableAroundPlayer",
                OptionInstance.cachedConstantTooltip(Component.translatable("chunkanimator.config.disableAroundPlayer.tooltip")),
                ChunkAnimatorConfig.disableAroundPlayer(),
                ChunkAnimatorConfig::setDisableAroundPlayer));
    }

    @Override
    public void removed() {
        super.removed();
        ChunkAnimatorConfig.save();
    }

    private static <E extends Enum<E>> OptionInstance<E> enumOption(final String key, final Class<E> type, final E[] values,
                                                                    final E current, final java.util.function.Consumer<E> setter) {
        final String prefix = "chunkanimator.config." + key;
        return new OptionInstance<>(
                prefix,
                value -> Tooltip.create(Component.translatable(prefix + "." + value.name().toLowerCase(Locale.ROOT) + ".tooltip")),
                // Cycle buttons already prefix the caption ("Animation: ..."), so only return the value.
                (caption, value) -> Component.translatable(prefix + "." + value.name().toLowerCase(Locale.ROOT)),
                new OptionInstance.Enum<>(Arrays.asList(values), enumCodec(type)),
                current,
                setter::accept);
    }

    private static OptionInstance<Integer> durationOption() {
        final int current = Math.min(ChunkAnimatorConfig.animationDuration(), DURATION_SLIDER_MAX) / DURATION_STEP;
        return new OptionInstance<>(
                "chunkanimator.config.animationDuration",
                OptionInstance.cachedConstantTooltip(Component.translatable("chunkanimator.config.animationDuration.tooltip")),
                (caption, steps) -> steps == 0
                        ? Options.genericValueLabel(caption, Component.translatable("options.off"))
                        : Options.genericValueLabel(caption, Component.translatable("chunkanimator.config.milliseconds", steps * DURATION_STEP)),
                new OptionInstance.IntRange(0, DURATION_SLIDER_MAX / DURATION_STEP),
                current,
                steps -> ChunkAnimatorConfig.setAnimationDuration(steps * DURATION_STEP));
    }

    private static <E extends Enum<E>> com.mojang.serialization.Codec<E> enumCodec(final Class<E> type) {
        return com.mojang.serialization.Codec.STRING.xmap(
                name -> Enum.valueOf(type, name.toUpperCase(Locale.ROOT)),
                value -> value.name());
    }

}
