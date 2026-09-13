package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerInteractBlockC2SPacket;
import thunder.hack.features.modules.Module;

public class PearlBlockThrow extends Module {
    public PearlBlockThrow() {
        super("PearlBlockThrow", Category.PLAYER);
    }

    @EventHandler
    public void onPackerSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundUseItemOnPacket p && mc.player.getMainHandItem().getItem() == Items.ENDER_PEARL)
            ((IPlayerInteractBlockC2SPacket) p).setHand(InteractionHand.OFF_HAND);
    }
}
