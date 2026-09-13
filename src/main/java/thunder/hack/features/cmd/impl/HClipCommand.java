package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("s").executes(context -> {
            final double x = -(Mth.sin(mc.player.getYRot() * Mth.DEG_TO_RAD) * 0.8);
            final double z = Mth.cos(mc.player.getYRot() * Mth.DEG_TO_RAD) * 0.8;

            for (int i = 0; i < 10; i++) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z, false, mc.player.horizontalCollision));
            }

            mc.player.setPos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);

            return SINGLE_SUCCESS;
        }));

        builder.then(arg("count", DoubleArgumentType.doubleArg()).executes(context -> {
            final double speed = context.getArgument("count", Double.class);

            try {
                sendMessage(ChatFormatting.GREEN + "клипаемся на  " + speed + " блоков.");
                mc.player.setPos(mc.player.getX() - ((double) Mth.sin(mc.player.getYRot() * Mth.DEG_TO_RAD) * speed), mc.player.getY(), mc.player.getZ() + (double) Mth.cos(mc.player.getYRot() * Mth.DEG_TO_RAD) * speed);
            } catch (Exception ignored) {
            }

            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            sendMessage("Попробуй .hclip <число>, .hclip s");
            return SINGLE_SUCCESS;
        });
    }
}
