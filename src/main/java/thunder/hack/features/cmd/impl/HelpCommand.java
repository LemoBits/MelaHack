package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.cmd.Command;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            sendMessage("Commands: \n");

            AtomicBoolean flip = new AtomicBoolean(false);

            Managers.COMMAND.getCommands().forEach(command -> {
                        mc.player.sendSystemMessage(Component.nullToEmpty(
                                (flip.get() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE)
                                        + Managers.COMMAND.getPrefix()
                                        + (flip.get() ? ChatFormatting.AQUA : ChatFormatting.DARK_AQUA)
                                        + command.getName()
                                        + (command.getAliases().isEmpty() ? "" : " (" + command.getAliases() + ")")
                                        + ChatFormatting.DARK_GRAY + " -> "
                                        + (flip.get() ? ChatFormatting.WHITE : ChatFormatting.GRAY)
                                        + command.getDescription()
                        ));
                        flip.set(!flip.get());
                    }
            );

            return SINGLE_SUCCESS;
        });
    }
}
