package ericthelemur.personalend.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.*;
public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        visit(dispatcher);
    }

    /**
     * Registers the visit command as
     * /end visit <player> or /end visit (sends to own)
     * or /end shared
     */
    public static void visit(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("end")
                .requires(ServerCommandSource::isExecutedByPlayer)
                .requires(requiresAdvancement("end/root"))
                .requires((s) -> PersonalEnd.CONFIG.endCommand)
                .then(
                    literal("visit").then(
                        argument("target", StringArgumentType.word())
                        .suggests(Commands::dimSuggester)
                        .executes(ctx -> visit(ctx, StringArgumentType.getString(ctx, "target")))
                    ).executes(ctx -> visit(ctx, null))
                ).then(
                    literal("shared")
                    .executes(ctx -> {
                        PersonalEnd.tpPlayerToSharedEnd(ctx.getSource().getPlayer());
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
    }

    /**
     * Predicate to check if the calling player holds the provided advancement (has the End advancement)
     */
    public static Predicate<ServerCommandSource> requiresAdvancement(String advancement) {
        return source -> {
            if (!PersonalEnd.CONFIG.gateCommandBehindAdvancement) return true;
            Advancement a = source.getServer().getAdvancementLoader().get(new Identifier(advancement));
            return source.getPlayer().getAdvancementTracker().getProgress(a).isDone();
        };
    }

    /**
     * Makes the command autocomplete suggest all created personal Ends
     */
    public static CompletableFuture<Suggestions> dimSuggester(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        var state = DragonPersistentState.getServerState(context.getSource().getServer());
        for (var s : state.getUsernames()) {
            builder.suggest(s);
        }
        return builder.buildFuture();
    }

    /**
     * The action of the /end visit command, if no target, send player to own End
     */
    private static int visit(CommandContext<ServerCommandSource> ctx, String target) throws CommandSyntaxException {
        var state = DragonPersistentState.getServerState(ctx.getSource().getServer());
        var uuid = target == null ? ctx.getSource().getPlayer().getUuid() : state.getUUID(target);
        if (uuid == null) {
            if (target == null || uuid == ctx.getSource().getPlayer().getUuid()) {
                throw new SimpleCommandExceptionType(Text.literal("You don't have a personal end.")).create();
            } else {
                throw new SimpleCommandExceptionType(Text.literal(target + " doesn't have a personal end.")).create();
            }
        }
        PersonalEnd.genAndGoToEnd(ctx.getSource().getPlayer(), uuid, state.getUsername(uuid));

        return Command.SINGLE_SUCCESS;
    }
}
