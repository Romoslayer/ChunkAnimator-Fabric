package lumien.chunkanimator;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.config.EasingFunction;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Client-side {@code /chunkanimator} command for changing settings in game. Every change is
 * applied immediately and saved to {@code config/chunkanimator.json}.
 */
public final class ChunkAnimatorCommand<S> {

    private final BiConsumer<S, Component> feedback;
    private final BiConsumer<S, Component> error;

    private ChunkAnimatorCommand(final BiConsumer<S, Component> feedback, final BiConsumer<S, Component> error) {
        this.feedback = feedback;
        this.error = error;
    }

    /**
     * Builds the command tree for a loader's client command source type.
     *
     * @param feedback sends a normal message to the source
     * @param error    sends an error message to the source
     */
    public static <S> LiteralArgumentBuilder<S> build(final BiConsumer<S, Component> feedback, final BiConsumer<S, Component> error) {
        return new ChunkAnimatorCommand<>(feedback, error).tree();
    }

    private LiteralArgumentBuilder<S> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private <T> RequiredArgumentBuilder<S, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private LiteralArgumentBuilder<S> tree() {
        return literal(ChunkAnimator.MOD_ID)
                .executes(this::show)
                .then(literal("reload").executes(context -> {
                    ChunkAnimatorConfig.load();
                    return show(context);
                }))
                .then(literal("config").executes(context -> {
                    // The chat screen closes after the command runs, so open the screen next tick.
                    ChunkAnimator.openConfigScreenNextTick();
                    return 1;
                }))
                .then(literal("mode")
                        .then(argument("mode", StringArgumentType.word())
                                .suggests(suggest(AnimationMode.values()))
                                .executes(context -> setEnum(context, "mode", AnimationMode.class, ChunkAnimatorConfig::setMode))))
                .then(literal("easing")
                        .then(argument("easing", StringArgumentType.word())
                                .suggests(suggest(EasingFunction.values()))
                                .executes(context -> setEnum(context, "easing", EasingFunction.class, ChunkAnimatorConfig::setEasingFunction))))
                .then(literal("duration")
                        .then(argument("milliseconds", IntegerArgumentType.integer(0, ChunkAnimatorConfig.MAX_DURATION))
                                .executes(context -> {
                                    ChunkAnimatorConfig.setAnimationDuration(IntegerArgumentType.getInteger(context, "milliseconds"));
                                    return saved(context);
                                })))
                .then(literal("disableAroundPlayer")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ChunkAnimatorConfig.setDisableAroundPlayer(BoolArgumentType.getBool(context, "enabled"));
                                    return saved(context);
                                })));
    }

    private <E extends Enum<E>> SuggestionProvider<S> suggest(final E[] values) {
        return (context, builder) -> {
            final String remaining = builder.getRemaining().toUpperCase(Locale.ROOT);
            Arrays.stream(values).map(Enum::name).filter(name -> name.startsWith(remaining)).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private <E extends Enum<E>> int setEnum(final CommandContext<S> context, final String argument,
                                                   final Class<E> type, final Consumer<E> setter) {
        final String raw = StringArgumentType.getString(context, argument);
        try {
            setter.accept(Enum.valueOf(type, raw.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            this.error.accept(context.getSource(), Component.literal("Unknown " + argument + ": " + raw + ". Options: "
                    + String.join(", ", Arrays.stream(type.getEnumConstants()).map(Enum::name).toList())));
            return 0;
        }
        return saved(context);
    }

    private int saved(final CommandContext<S> context) {
        ChunkAnimatorConfig.save();
        return show(context);
    }

    private int show(final CommandContext<S> context) {
        this.feedback.accept(context.getSource(), Component.literal(String.format(Locale.ROOT,
                "Chunk Animator: mode=%s, easing=%s, duration=%dms, disableAroundPlayer=%s",
                ChunkAnimatorConfig.mode(), ChunkAnimatorConfig.easingFunction(),
                ChunkAnimatorConfig.animationDuration(), ChunkAnimatorConfig.disableAroundPlayer())));
        return 1;
    }

}
