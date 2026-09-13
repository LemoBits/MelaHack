package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import thunder.hack.features.cmd.Command;
import thunder.hack.core.manager.client.ConfigManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class TabParseCommand extends Command {
    public TabParseCommand() {
        super("tabparse");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            String serverIP = "unknown_server";
            if (mc.getConnection().getServerData() != null && mc.getConnection().getServerData().ip != null)
                serverIP = mc.getConnection().getServerData().ip.replace(':', '_');

            String randomSuffix = generateRandomString(5);

            File dir = new File(ConfigManager.TABPARSER_FOLDER, serverIP);

            if (!dir.exists()) dir.mkdirs();

            String fileName = serverIP + "-" + new SimpleDateFormat("dd.MM.yyyy").format(new Date()) + "-" + randomSuffix + ".txt";
            File file = new File(dir, fileName);

            try {
                file.createNewFile();
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                writer.write("========================\n\n");
                writer.write("Server: " + mc.getConnection().getServerData().ip + "\n");
                writer.write("Date: " + new SimpleDateFormat("dd.MM.yyyy").format(new Date()) + "\n\n");
                writer.write("========================\n\n");

                List<PlayerInfo> sortedPlayers = new ArrayList<>(mc.getConnection().getOnlinePlayers());
                sortedPlayers.sort((player1, player2) -> {
                    String prefix1 = player1.getTeam().getPlayerPrefix().getString();
                    String prefix2 = player2.getTeam().getPlayerPrefix().getString();
                    return prefix2.compareTo(prefix1);
                });

                for (PlayerInfo entry : sortedPlayers)
                    writer.write(PlayerTeam.formatNameForTeam(entry.getTeam(), Component.literal(entry.getProfile().name())).getString() + "\n");

                writer.close();
                sendMessage(isRu() ? ChatFormatting.GREEN + "Таб успешно сохранен в " + file.getPath() : ChatFormatting.GREEN + "Tab was successfully saved in " + file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return SINGLE_SUCCESS;
        });
    }


    private String generateRandomString(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    private String getPlayerPrefix(PlayerInfo playerInfo) {
        return playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().getString() : "";
    }
}
