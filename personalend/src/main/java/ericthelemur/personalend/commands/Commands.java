package ericthelemur.personalend.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
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
                        argument("targets", StringArgumentType.word())
                            .suggests(new EndDimOwnerSuggester())
                            .executes(ctx -> {
                                ctx.getSource().sendMessage(Text.literal(String.format("Visiting {}'s end")));

                                return 0;
                            })
                ))
        );
    }
}
