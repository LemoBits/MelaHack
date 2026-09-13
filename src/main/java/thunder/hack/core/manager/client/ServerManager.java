package thunder.hack.core.manager.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.IManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.utility.math.MathUtility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;

public class ServerManager implements IManager {
    private final ArrayDeque<Float> tpsResult = new ArrayDeque<>(20);
    private long time;
    private long tickTime;
    private float tps;

    public float getTPS() {
        return round2(tps);
    }

    public float getTPS2() {
        return round2(20.0f * ((float) tickTime / 1000f));
    }

    public float getTPSFactor() {
        return (float) tickTime / 1000f;
    }

    public static float round2(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            if (time != 0L) {
                tickTime = System.currentTimeMillis() - time;

                if (tpsResult.size() > 20)
                    tpsResult.poll();

                tpsResult.add(20.0f * (1000.0f / (float) (tickTime)));

                float average = 0.0f;

                for (Float value : tpsResult) average += MathUtility.clamp(value, 0f, 20f);

                tps = average / (float) tpsResult.size();
            }
            time = System.currentTimeMillis();
        }
    }

    public int getPing() {
        if (mc.getConnection() == null || mc.player == null) return 0;

        if (ModuleManager.fastLatency.isEnabled())
            return ModuleManager.fastLatency.resolvedPing;

        PlayerInfo playerListEntry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }
}
