package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import thunder.hack.features.cmd.Command;
import thunder.hack.utility.world.ExplosionUtility;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class BenchMarkCommand extends Command {
    public BenchMarkCommand() {
        super("benchmark");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {

            new Thread(() -> {
                long time = System.currentTimeMillis();

                BlockPos playerPos = BlockPos.containing(mc.player.position());
                int r = 6;

                for (int x = playerPos.getX() - r; x <= playerPos.getX() + r; x++)
                    for (int y = playerPos.getY() - r; y <= playerPos.getY() + r; y++)
                        for (int z = playerPos.getZ() - r; z <= playerPos.getZ() + r; z++) {
                            float dmg = ExplosionUtility.getExplosionDamage(new Vec3(x, y, z), mc.player, false);
                        }

                time = System.currentTimeMillis() - time;
                int score = (int) ((216f / (float) time) * 10000);
                sendMessage((isRu() ? "Твой CPU набрал: " : "Your CPU score: ") + ChatFormatting.GRAY + score);
            }).start();

            return SINGLE_SUCCESS;
        });
    }
}
