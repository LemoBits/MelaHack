package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public class AntiCrash extends Module { //https://github.com/Bram1903/MinecraftPlayerCrasher
    public final Setting<Boolean> debug = new Setting<>("Debug", false);

    private Timer debugTimer = new Timer();

    public AntiCrash() {
        super("AntiCrash", Category.CLIENT);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive receive) {
        if (receive.getPacket() instanceof ClientboundExplodePacket exp) {
            if (exp.center().x > 1E9 || exp.center().y > 1E9 || exp.center().z > 1E9
                || exp.playerKnockback().map(vec -> Math.abs(vec.x) > 1E9 || Math.abs(vec.y) > 1E9 || Math.abs(vec.z) > 1E9).orElse(false)) {
                if (debug.getValue() && debugTimer.passedMs(1000)) {
                    sendMessage("ExplosionS2CPacket canceled");
                    debugTimer.reset();
                }
                receive.cancel();
            }
        } else if (receive.getPacket() instanceof ClientboundLevelParticlesPacket p && (p.getX() > 1E9 || p.getY() > 1E9 || p.getZ() > 1E9 || p.getMaxSpeed() > 1E9 || p.getXDist() > 1E9 || p.getYDist() > 1E9 || p.getZDist() > 1E9)) {
            if (debug.getValue() && debugTimer.passedMs(1000)) {
                sendMessage("ParticleS2CPacket canceled");
                debugTimer.reset();
            }
            receive.cancel();
        } else if (receive.getPacket() instanceof ClientboundPlayerPositionPacket pos) {
            if (pos.change().position().x > 1E9 || pos.change().position().y > 1E9 || pos.change().position().z > 1E9
                || pos.change().yRot() > 1E9 || pos.change().xRot() > 1E9) {
                if (debug.getValue() && debugTimer.passedMs(1000)) {
                    sendMessage("PlayerPositionLookS2CPacket canceled");
                    debugTimer.reset();
                }
                receive.cancel();
            }
        }
    }
}
