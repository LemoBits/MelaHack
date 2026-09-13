package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;

public class XCarry extends Module {
    public XCarry() {
        super("XCarry", Category.PLAYER);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof ServerboundContainerClosePacket) e.cancel();
    }
}
