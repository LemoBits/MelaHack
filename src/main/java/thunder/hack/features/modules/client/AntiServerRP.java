package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.math.MathUtility;

public final class AntiServerRP extends Module {
    public AntiServerRP() {
        super("AntiServerRP", Category.CLIENT);
    }

    private boolean confirm, accepted;
    private int delay;

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof ClientboundResourcePackPushPacket) {
            confirm = true;
            accepted = false;
            delay = 0;
            e.cancel();
        }
    }

    @Override
    public void onUpdate() {
        if(confirm) {
            delay++;

            if(delay > MathUtility.random(15, 30) && !accepted) {
                sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), ServerboundResourcePackPacket.Action.ACCEPTED));
                accepted = true;
            }

            if(delay > MathUtility.random(40, 60) && accepted) {
                sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
                confirm = false;
            }
        }
    }
}
