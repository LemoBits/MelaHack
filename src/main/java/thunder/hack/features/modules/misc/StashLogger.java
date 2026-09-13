package thunder.hack.features.modules.misc;

import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ConfigManager;
import thunder.hack.gui.notification.Notification;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.render.StorageEsp;
import thunder.hack.setting.Setting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class StashLogger extends Module {
    private final Setting<Boolean> sound = new Setting<>("Sound", true);
    private final Setting<Boolean> saveToFile = new Setting<>("SaveToFile", true);
    private final Setting<Integer> minChests = new Setting<>("MinChests", 5, 0, 100);
    private final Setting<Integer> minShulkers = new Setting<>("MinShulkers", 0, 0, 100);

    private List<LevelChunk> savedChunks = new ArrayList<>();

    public StashLogger() {
        super("StashLogger", Category.MISC);
    }

    @Override
    public void onEnable() {
        savedChunks.clear();
    }

    @Override
    public void onUpdate() {
        for (LevelChunk chunk : StorageEsp.getLoadedChunks()) {
            if (savedChunks.contains(chunk))
                continue;

            List<BlockEntity> storages = chunk.getBlockEntities().values().stream().toList();

            int chests = 0;
            int shulkers = 0;

            for (BlockEntity storage : storages) {
                if (storage instanceof ChestBlockEntity)
                    chests++;
                if (storage instanceof ShulkerBoxBlockEntity)
                    shulkers++;
            }

            if (chests >= minChests.getValue() && shulkers >= minShulkers.getValue()) {
                savedChunks.add(chunk);

                String str = "Stash pos: X:" + chunk.getPos().getMiddleBlockX() + " Z:" + chunk.getPos().getMiddleBlockZ() + " Chests: " + chests + " Shulkers: " + shulkers;
                Managers.NOTIFICATION.publicity("StashLogger", str, 5, Notification.Type.SUCCESS);
                sendMessage(str);

                if (sound.getValue())
                    mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1f);

                String serverIP = "unknown_server";
                if (mc.getConnection().getServerData() != null && mc.getConnection().getServerData().ip != null)
                    serverIP = mc.getConnection().getServerData().ip.replace(':', '_');

                if (saveToFile.getValue())
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(ConfigManager.STASHLOGGER_FOLDER, serverIP + ".txt"), true));
                        writer.append("\nStash pos: X:" + chunk.getPos().getMiddleBlockX() + " Z:" + chunk.getPos().getMiddleBlockZ() + " Chests: " + chests + " Shulkers: " + shulkers);
                        writer.close();
                    } catch (Exception e) {
                    }
            }
        }
    }
}
