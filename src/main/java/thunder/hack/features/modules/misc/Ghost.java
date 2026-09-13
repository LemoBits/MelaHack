package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class Ghost extends Module {
    public Ghost() {
        super("Ghost", Category.MISC);
    }

    private boolean bypass = false;

    @Override
    public void onEnable() {
        bypass = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.respawn();
        bypass = false;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.getHealth() == 0.0f) {
            mc.player.setHealth(20.0f);
            bypass = true;
            mc.gui.setScreen(null);
            mc.player.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            sendMessage(isRu() ? "Для возрождения выключи модуль!" : "To revive, turn off the module!");
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (bypass && event.getPacket() instanceof ServerboundMovePlayerPacket) event.cancel();
    }
}