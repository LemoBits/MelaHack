package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.modules.client.ClientSettings;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("down").executes(context -> {
            int i;
            float y = 0.0f;

            for (i = 1; i < 255; ++i) {
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.AIR.defaultBlockState()) {
                    y = -i - 1;
                    break;
                }

                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) != Blocks.BEDROCK.defaultBlockState())
                    continue;

                sendMessage(ChatFormatting.RED + (isRu() ? "Некуда клипать!" : "There's nowhere to clip!"));
                return SINGLE_SUCCESS;
            }

            clip(y);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("up").executes(context -> {
            int i;
            float y = 0.0f;

            for (i = 4; i < 255; ++i) {
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) != Blocks.AIR.defaultBlockState())
                    continue;
                y = i + 1;
                break;
            }

            clip(y);
            return SINGLE_SUCCESS;
        }));

        builder.then(arg("count", DoubleArgumentType.doubleArg()).executes(context -> {
            final double count = context.getArgument("count", Double.class);

            try {
                sendMessage(ChatFormatting.GREEN + "Клипаемся на " + count + " блоков");
                clip(count);
            } catch (Exception ignored) {
            }

            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            sendMessage("Попробуй .vclip <число>");
            return SINGLE_SUCCESS;
        });
    }

    private void clip (double b) {
        if (ClientSettings.clipCommandMode.getValue() == ClientSettings.ClipCommandMode.Matrix) {
            for (int i = 0; i < 10; ++i)
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));

            for (int i = 0; i < 10; ++i)
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ(), false, mc.player.horizontalCollision));
        } else {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ(), false, mc.player.horizontalCollision));
        }
        mc.player.setPos(mc.player.getX(), mc.player.getY() + b, mc.player.getZ());
    }
}
