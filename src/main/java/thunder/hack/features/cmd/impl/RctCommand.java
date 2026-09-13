package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.scores.Objective;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.cmd.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class RctCommand extends Command {
    public RctCommand() {
        super("rct");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            String sName = mc.player.connection.getServerData() == null ? "none" : mc.player.connection.getServerData().ip;

            if (!sName.contains("funtime") && !sName.contains("spookytime")) {
                sendMessage(isRu() ? "Rct работает только на фанике и спуки" : "Rct works only on funtime and spookytime");
                return SINGLE_SUCCESS;
            }

            String an = "an" + ((Objective) mc.player.getScoreboard().getObjectives().toArray()[0]).getDisplayName().getString().substring(10);

            Managers.ASYNC.run(() -> {
                mc.player.connection.sendCommand("hub");
                long failSafe = System.currentTimeMillis();
                while (ThunderHack.core.getSetBackTime() > 600) {
                    if (System.currentTimeMillis() - failSafe > 1000)
                        break;
                }
                mc.player.connection.sendCommand(an);
            });
            return SINGLE_SUCCESS;
        });
    }
}
