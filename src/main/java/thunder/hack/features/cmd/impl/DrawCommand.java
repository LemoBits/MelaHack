package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.cmd.args.ModuleArgumentType;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DrawCommand extends Command {
    public DrawCommand() {
        super("draw");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(arg("module", ModuleArgumentType.create()).executes(context -> {
            Module module = context.getArgument("module", Module.class);

            module.setDrawn(!module.isDrawn());

            if(ClientSettings.isRu()){
                sendMessage("Модуль " + ChatFormatting.GREEN + module.getName() + ChatFormatting.WHITE + " теперь " + (module.isDrawn() ? "виден в ArrayList" : "не виден в ArrayList"));
            } else {
                sendMessage(ChatFormatting.GREEN + module.getName() + ChatFormatting.WHITE + " is now " + (module.isDrawn() ? "visible in ArrayList" : "invisible in ArrayList"));
            }

            return SINGLE_SUCCESS;
        }));
    }
}
