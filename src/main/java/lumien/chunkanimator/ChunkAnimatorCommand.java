package lumien.chunkanimator;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import lumien.chunkanimator.config.AnimationMode;
import lumien.chunkanimator.config.ChunkAnimatorConfig;
import lumien.chunkanimator.config.EasingFunction;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Client-side {@code /chunkanimator} command for changing settings in game. Every change is
 * applied immediately and saved to {@code config/chunkanimator.json}.
 */
final class ChunkAnimatorCommand {

    private ChunkAnimatorCommand() {}

    static void register(final CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal(ChunkAnimator.MOD_ID)
                .executes(ChunkAnimatorCommand::show)
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
                                }))));
    }

    private static <E extends Enum<E>> SuggestionProvider<FabricClientCommandSource> suggest(final E[] values) {
        return (context, builder) -> {
            final String remaining = builder.getRemaining().toUpperCase(Locale.ROOT);
            Arrays.stream(values).map(Enum::name).filter(name -> name.startsWith(remaining)).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static <E extends Enum<E>> int setEnum(final CommandContext<FabricClientCommandSource> context, final String argument,
                                                   final Class<E> type, final Consumer<E> setter) {
        final String raw = StringArgumentType.getString(context, argument);
        try {
            setter.accept(Enum.valueOf(type, raw.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Component.literal("Unknown " + argument + ": " + raw + ". Options: "
                    + String.join(", ", Arrays.stream(type.getEnumConstants()).map(Enum::name).toList())));
            return 0;
        }
        return saved(context);
    }

    private static int saved(final CommandContext<FabricClientCommandSource> context) {
        ChunkAnimatorConfig.save();
        return show(context);
    }

    private static int show(final CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal(String.format(Locale.ROOT,
                "Chunk Animator: mode=%s, easing=%s, duration=%dms, disableAroundPlayer=%s",
                ChunkAnimatorConfig.mode(), ChunkAnimatorConfig.easingFunction(),
                ChunkAnimatorConfig.animationDuration(), ChunkAnimatorConfig.disableAroundPlayer())));
        return 1;
    }

}
