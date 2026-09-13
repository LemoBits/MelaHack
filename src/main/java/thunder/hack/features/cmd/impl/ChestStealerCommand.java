package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.cmd.args.ChestStealerArgumentType;
import thunder.hack.core.manager.client.ModuleManager;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ChestStealerCommand extends Command {
    public ChestStealerCommand() {
        super("cheststealer", "stealer", "rat");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("reset").executes(context -> {
            ModuleManager.chestStealer.items.getValue().getItemsById().clear();
            sendMessage("ChestStealer got reset.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
            String blockName = context.getArgument("item", String.class);

            String result = getRegistered(blockName);
            if(result != null){
                ModuleManager.chestStealer.items.getValue().getItemsById().add(result);
                sendMessage(ChatFormatting.GREEN + blockName + (isRu() ? " добавлен в ChestStealer" : " added to ChestStealer"));
            } else {
                sendMessage(ChatFormatting.RED + (isRu() ? "Такого предмета нет!" : "There is no such item!"));
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("del").then(arg("item", ChestStealerArgumentType.create()).executes(context -> {
            String blockName = context.getArgument("item", String.class);

            String result = getRegistered(blockName);
            if(result != null){
                ModuleManager.chestStealer.items.getValue().getItemsById().remove(result);
                sendMessage(ChatFormatting.GREEN + blockName + (isRu() ? " удален из ChestStealer" : " removed from ChestStealer"));
            } else {
                sendMessage(ChatFormatting.RED + (isRu() ? "Такого предмета нет!" : "There is no such item!"));
            }
            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            if (ModuleManager.chestStealer.items.getValue().getItemsById().isEmpty()) {
                sendMessage("ChestStealer list empty");
            } else {
                StringBuilder f = new StringBuilder("ChestStealer list: ");

                for (String name :  ModuleManager.chestStealer.items.getValue().getItemsById())
                    try {
                        f.append(name).append(", ");
                    } catch (Exception ignored) {
                    }

                sendMessage(f.toString());
            }

            return SINGLE_SUCCESS;
        });
    }

    public static String getRegistered(String Name) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.getDescriptionId().replace("block.minecraft.","").equalsIgnoreCase(Name)) {
                return block.getDescriptionId().replace("block.minecraft.","");
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.getDescriptionId().replace("item.minecraft.","").equalsIgnoreCase(Name)) {
                return item.getDescriptionId().replace("item.minecraft.","");
            }
        }
        return null;
    }
}
