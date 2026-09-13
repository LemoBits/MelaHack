package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerPositionS2CPacket;
import thunder.hack.features.modules.Module;

public class NoServerRotate extends Module {
    public NoServerRotate() {
        super("NoServerRotate", Category.PLAYER);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof PlayerPositionS2CPacket pac) {
            EntityPosition change = pac.change();
            EntityPosition updated = new EntityPosition(change.position(), change.deltaMovement(), mc.player.getYaw(), mc.player.getPitch());
            ((IPlayerPositionS2CPacket) (Object) pac).setChange(updated);
        }
    }
}
