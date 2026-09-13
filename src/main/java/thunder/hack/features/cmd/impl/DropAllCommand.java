package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class DropAllCommand extends Command {
    public DropAllCommand() {
        super("dropall", "drop");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("legit").executes(context -> {
            Managers.ASYNC.run(() -> {
                for (int i = 5; i <= 45; i++) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);
                    try {
                        Thread.sleep(70);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            }, 1);
            sendMessage("ok");
            return SINGLE_SUCCESS;
        }));


        builder.executes(context -> {
            for (int i = 5; i <= 45; i++)
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);
            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            sendMessage("ok");
            return SINGLE_SUCCESS;
        });
    }
}
