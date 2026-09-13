package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;

public class NoServerSlot extends Module {
    public NoServerSlot() {
        super("NoServerSlot", Category.PLAYER);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundSetHeldSlotPacket) {
            event.cancel();
            sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
        }
    }
}
