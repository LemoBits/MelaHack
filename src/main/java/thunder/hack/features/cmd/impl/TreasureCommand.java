package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class TreasureCommand extends Command {
    public TreasureCommand() {
        super("gettreasure", "treasure");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.player.getMainHandItem().getItem().toString().equals("filled_map")) {
                Record nbt = mc.player.getMainHandItem().getOrDefault(DataComponents.MAP_DECORATIONS, PotionContents.EMPTY);
                if (nbt == null) return SINGLE_SUCCESS;

                StringBuilder result = new StringBuilder();
                String rawNbt = nbt.toString();
                for (int i = rawNbt.indexOf("x="); i < rawNbt.indexOf(", rotation") - 2; i++)
                    result.append(rawNbt.charAt(i));
                sendMessage(isRu() ? "Нашел! Координаты: " + result : "Found! Coords: " + result);
            } else sendMessage(isRu() ? "Возьми карту в руки!" : "Take a map into your hand!");

            return SINGLE_SUCCESS;
        });
    }
}
