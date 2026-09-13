package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerPositionLookS2CPacket;
import thunder.hack.features.modules.Module;

public class NoServerRotate extends Module {
    public NoServerRotate() {
        super("NoServerRotate", Category.PLAYER);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket pac) {
            PositionMoveRotation change = pac.change();
            PositionMoveRotation updated = new PositionMoveRotation(change.position(), change.deltaMovement(), mc.player.getYRot(), mc.player.getXRot());
            ((IPlayerPositionLookS2CPacket) (Object) pac).setChange(updated);
        }
    }
}
