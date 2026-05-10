package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;

public class Freeze extends Module {

    public Freeze() {
        super("Freeze", Category.MOVEMENT);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(EventMove e) {

            e.setX(0.);
            e.setY(0.);
            e.setZ(0.);
            e.cancel();

    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof PlayerMoveC2SPacket)
            e.cancel();

    }

}
