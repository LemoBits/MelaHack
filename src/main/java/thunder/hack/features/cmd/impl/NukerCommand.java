package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.cmd.args.SearchArgumentType;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class NukerCommand extends Command {
    public NukerCommand() {
        super("nuker");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("reset").executes(context -> {
            ModuleManager.nuker.selectedBlocks.getValue().clear();
            sendMessage(isRu() ? "Все блоки были удалены!" : "Nuker got reset!");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(arg("block", SearchArgumentType.create()).executes(context -> {
            String blockName = context.getArgument("block", String.class);

            Block result = getRegisteredBlock(blockName);
            if (result != null) {
                ModuleManager.nuker.selectedBlocks.getValue().add(result);
                sendMessage(ChatFormatting.GREEN + blockName + (isRu() ? " добавлен в Nuker" : " added to Nuker"));
            } else {
                sendMessage(ChatFormatting.RED + (isRu() ? "Такого блока нет!" : "There is no such block!"));
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("del").then(arg("block", SearchArgumentType.create()).executes(context -> {
            String blockName = context.getArgument("block", String.class);

            Block result = getRegisteredBlock(blockName);
            if (result != null) {
                ModuleManager.nuker.selectedBlocks.getValue().remove(blockName);
                sendMessage(ChatFormatting.GREEN + blockName + (isRu() ? " удален из Nuker" : " removed from Nuker"));
            } else {
                sendMessage(ChatFormatting.RED + (isRu() ? "Такого блока нет!" : "There is no such block!"));
            }

            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            if (ModuleManager.nuker.selectedBlocks.getValue().getItemsById().isEmpty()) {
                sendMessage("Nuker list empty");
            } else {
                StringBuilder f = new StringBuilder("Nuker list: ");

                for (String name : ModuleManager.nuker.selectedBlocks.getValue().getItemsById())
                    try {
                        f.append(name).append(", ");
                    } catch (Exception ignored) {
                    }

                sendMessage(f.toString());
            }

            return SINGLE_SUCCESS;
        });
    }

    public static Block getRegisteredBlock(String blockName) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.getDescriptionId().replace("block.minecraft.", "").equalsIgnoreCase(blockName.replace("block.minecraft.", ""))) {
                return block;
            }
        }
        return null;
    }
}
