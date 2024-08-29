package ericthelemur.personalend.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ericthelemur.personalend.Config;
import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static ericthelemur.personalend.PersonalEnd.isAnyEnd;
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
                .requires((s) -> PersonalEnd.CONFIG.endCommand || s.hasPermissionLevel(3))
                .then(
                    literal("visit")
                    .then(
                        argument("target", StringArgumentType.word())
                        .suggests(Commands::dimSuggester)
                        .executes(ctx -> {
                            if (sourceOnEndPlatformCheck(ctx.getSource())) visit(ctx, StringArgumentType.getString(ctx, "target"));
                            return Command.SINGLE_SUCCESS;
                        })
                    ).executes(ctx -> {
                        if (sourceOnEndPlatformCheck(ctx.getSource())) visit(ctx, null);
                        return Command.SINGLE_SUCCESS;
                    })
                ).then(
                    literal("shared")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(ctx -> {
                        if (sourceOnEndPlatformCheck(ctx.getSource())) PersonalEnd.tpPlayerToSharedEnd(ctx.getSource().getPlayer());
                        return Command.SINGLE_SUCCESS;
                    })
                ).then(
                    literal("reload")
                    .requires((s) -> s.hasPermissionLevel(3))
                    .executes(ctx -> {
                        Config.load();
                        var s = ctx.getSource();
                        if (s.getServer() != null)
                            s.getServer().getPlayerManager().sendCommandTree(s.getPlayer());
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
            AdvancementEntry a = source.getServer().getAdvancementLoader().get(Identifier.ofVanilla(advancement));
            var p = source.getPlayer();
            if (p == null) return false;
            return p.getAdvancementTracker().getProgress(a).isDone();
        };
    }

    /**
     * Predicate to check if the calling player is standing on the End Platform (approx)
     */
    public static Boolean standingOnEndPlatform(ServerCommandSource source) {
        if (!PersonalEnd.CONFIG.commandOnEndPlatformOnly || source.hasPermissionLevel(3)) return true;
        var p = source.getPlayer();
        if (p == null) return false;
        return isAnyEnd(p.getWorld()) && p.getPos().distanceTo(ServerWorld.END_SPAWN_POS.toCenterPos()) < 10;
    }

    public static Boolean sourceOnEndPlatformCheck(ServerCommandSource source) throws CommandSyntaxException {
        if (!standingOnEndPlatform(source)) {
            throw new SimpleCommandExceptionType(Text.literal("You need to be standing on the End platform to visit other Ends")).create();
        } else {
            return true;
        }
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
        var player = ctx.getSource().getPlayer();
        var tt = PersonalEnd.genAndGoToEnd(player, uuid, state.getUsername(uuid));
        player.teleportTo(tt);

        return Command.SINGLE_SUCCESS;
    }
}
