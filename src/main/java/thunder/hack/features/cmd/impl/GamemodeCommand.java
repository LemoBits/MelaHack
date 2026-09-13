package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.level.GameType;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GamemodeCommand extends Command {
    public GamemodeCommand() {
        super("gamemode", "gm");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(arg("mode", StringArgumentType.greedyString()).executes(context -> {
            final String mode = context.getArgument("mode", String.class);

            switch (mode) {
                case "survival", "0":
                    mc.gameMode.setLocalMode(GameType.SURVIVAL);
                case "creative", "1":
                    mc.gameMode.setLocalMode(GameType.CREATIVE);
                case "spectator", "2":
                    mc.gameMode.setLocalMode(GameType.SPECTATOR);
                case "adventure", "3":
                    mc.gameMode.setLocalMode(GameType.ADVENTURE);
            }

            return SINGLE_SUCCESS;
        }));
    }
}
