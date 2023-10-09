package ericthelemur.personalend.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;
public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        visit(dispatcher);
    }

    public static void visit(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("end").then(literal("visit").then(
                        argument("target", StringArgumentType.word())
                            .suggests(new EndDimOwnerSuggester())
                            .executes(ctx -> {
                                var state = DragonPersistentState.getServerState(ctx.getSource().getServer());
                                var target = StringArgumentType.getString(ctx, "target");
                                var uuid = state.getUUID(target);
                                if (uuid == null) {
                                    throw new SimpleCommandExceptionType(Text.literal(target + " doesn't have a end.")).create();
                                }
                                PersonalEnd.genAndGoToEnd(ctx.getSource().getPlayer(), uuid, state.getUsername(uuid));

                                return Command.SINGLE_SUCCESS;
                            })
                ))
        );
    }
}
