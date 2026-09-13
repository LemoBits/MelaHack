package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.utility.player.InventoryUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class EClipCommand extends Command {
    public EClipCommand() {
        super("eclip");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("bedrock").executes(context -> {
            execute(-((float) mc.player.getY()) - 3.0f);
            return SINGLE_SUCCESS;
        }).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
            float y = -((float) mc.player.getY()) - 3.0f;

            if (y == 0.0f) y = context.getArgument("number", Float.class);
            execute(y);

            return SINGLE_SUCCESS;
        })));

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

                sendMessage(ChatFormatting.RED + " можно телепортироваться только под бедрок");
                sendMessage(ChatFormatting.RED + " eclip bedrock");
                return SINGLE_SUCCESS;
            }

            execute(y);
            return SINGLE_SUCCESS;
        }).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
            int i;
            float y = 0.0f;

            for (i = 1; i < 255; ++i) {
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) == Blocks.AIR.defaultBlockState()) {
                    y = -i - 1;
                    break;
                }

                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, -i, 0)) != Blocks.BEDROCK.defaultBlockState())
                    continue;

                sendMessage(ChatFormatting.RED + " можно телепортироваться только под бедрок");
                sendMessage(ChatFormatting.RED + " eclip bedrock");
                return SINGLE_SUCCESS;
            }

            if (y == 0.0f) y = context.getArgument("number", Float.class);

            execute(y);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("up").executes(context -> {
            int i;
            float y = 0.0f;

            for (i = 4; i < 255; ++i) {
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) != Blocks.AIR.defaultBlockState())
                    continue;
                y = i + 1;
                break;
            }

            execute(y);
            return SINGLE_SUCCESS;
        }).then(arg("number", FloatArgumentType.floatArg()).executes(context -> {
            int i;
            float y = 0.0f;

            for (i = 4; i < 255; ++i) {
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, i, 0)) != Blocks.AIR.defaultBlockState())
                    continue;
                y = i + 1;
                break;
            }

            if (y == 0.0f) y = context.getArgument("number", Float.class);
            execute(y);

            return SINGLE_SUCCESS;
        })));
    }

    private void execute(float y) {
        int elytra;

        if ((elytra = InventoryUtility.findItemInInventory(Items.ELYTRA).slot()) == -1) {
            sendMessage(ChatFormatting.RED + "вам нужны элитры в инвентаре");
            return;
        }
        if (elytra != -2) {
            mc.gameMode.handleContainerInput(0, elytra, 1, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
        }

        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision));
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + (double) y, mc.player.getZ(), false, mc.player.horizontalCollision));
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

        if (elytra != -2) {
            mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
            mc.gameMode.handleContainerInput(0, elytra, 1, ContainerInput.PICKUP, mc.player);
        }

        mc.player.setPos(mc.player.getX(), mc.player.getY() + (double) y, mc.player.getZ());
    }
}
