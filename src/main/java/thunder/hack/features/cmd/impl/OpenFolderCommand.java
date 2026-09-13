package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;

import java.io.File;
import net.minecraft.Util;
import net.minecraft.commands.SharedSuggestionProvider;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class OpenFolderCommand extends Command {
    public OpenFolderCommand() {
        super("openfolder", "folder");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            Util.getPlatform().openFile(new File("ThunderHackRecode/configs/"));
            return SINGLE_SUCCESS;
        });
    }
}
