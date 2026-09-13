package thunder.hack.features.modules.client;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ConfigManager;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.math.MathUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.platform.IconSet;

public class UnHook extends Module { // Йо фабос, засунь в о4ко себе фалос
    public UnHook() {
        super("UnHook", Category.CLIENT);
    }

    List<Module> list;

    public int code = 0;

    @Override
    public void onEnable() {
        code = (int) MathUtility.random(10, 99);
        for (int i = 0; i < 20; i++)
            sendMessage(isRu() ? ChatFormatting.RED + "Ща все свернется, напиши в чат " + ChatFormatting.WHITE + code + ChatFormatting.RED + " чтобы все вернуть!"
                    : ChatFormatting.RED + "It's all close now, write to the chat " + ChatFormatting.WHITE + code + ChatFormatting.RED + " to return everything!");

        list = Managers.MODULE.getEnabledModules();

        mc.setScreen(null);

        Managers.ASYNC.run(() -> {
            mc.executeIfPossible(() -> {
                for (Module module : list) {
                    if (module.equals(this))
                        continue;
                    module.disable();
                }
                ClientSettings.customMainMenu.setValue(false);

                // Clean icon
                try {
                    mc.getWindow().setIcon(mc.getVanillaPackResources(), SharedConstants.getCurrentVersion().stable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
                } catch (Exception e) {
                }

                // Clean chat
                mc.gui.getChat().clearMessages(true);
                setEnabled(true);

                // Clean log
                try {
                    File file = new File(mc.gameDirectory + File.separator + "logs" + File.separator + "latest.log");
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                    ArrayList<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if(line.contains("thunderhack") || line.contains("ThunderHack") || line.contains("$$") || line.contains("\\______/")
                                || line.contains("By pan4ur, 06ED") || line.contains("\u26A1") || line.contains("thunder.hack"))
                            continue;
                        lines.add(line);
                    }
                    fis.close();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                        for (String s : lines)
                            writer.write(s + "\n");
                    } catch (Exception ignored) {
                    }

                    // Rename cfg dir
                    ConfigManager.MAIN_FOLDER.renameTo(new File("XaeroWaypoints_BACKUP092738"));
                } catch (IOException ignored) {
                }
            });
        }, 5000);
    }

    @Override
    public void onDisable() {
        if (list == null)
            return;

        for (Module module : list) {
            if (module.equals(this))
                continue;
            module.enable();
        }
        ClientSettings.customMainMenu.setValue(true);

        // Rename cfg dir back
        try {
            new File("XaeroWaypoints_BACKUP092738").renameTo(new File("ThunderHackRecode"));
        } catch (Exception e) {
        }
    }
}
