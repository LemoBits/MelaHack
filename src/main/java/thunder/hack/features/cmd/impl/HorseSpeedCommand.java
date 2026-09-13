package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.Horse;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class HorseSpeedCommand extends Command {
    public HorseSpeedCommand() {
        super("gethorsespeed", "horsespeed");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            if (mc.player.getVehicle() != null && mc.player.getVehicle() instanceof Horse horse) {
                if (horse.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
                    if (isRu()) sendMessage(ChatFormatting.RED + "У тебя нет седла!");
                    else sendMessage(ChatFormatting.RED + "You don't have a saddle!");
                    return SINGLE_SUCCESS;
                }

                float speed = horse.zza * 43.17f;

                float ratio = speed / 14.512f;

                String verbose = "";

                if (ratio < 0.3)
                    verbose = isRu() ? "У тебя кринжовая лошадь :(" : "Your horse is shitty :(";

                if (ratio > 0.3 && ratio < 0.6)
                    verbose = isRu() ? "У тебя нормальная лошадь" : "Your horse is normal";

                if (ratio > 0.6)
                    verbose = isRu() ? "У тебя пиздатая лошадь :)" : "Your horse is good :)";

                if (ratio > 0.9)
                    verbose = isRu() ? "Цыганы уже в пути" : "Your horse is very good :)";

                if (isRu()) sendMessage(ChatFormatting.GREEN + "Скорость лошади: " + speed + " из 14.512. " + verbose);
                else sendMessage(ChatFormatting.GREEN + "Horse speed: " + speed + " out of 14.512. " + verbose);
            } else {
                if (isRu()) sendMessage(ChatFormatting.RED + "У тебя нет лошади!");
                else sendMessage(ChatFormatting.RED + "You don't have a horse!");
            }
            return SINGLE_SUCCESS;
        });
    }
}
