package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.projectile.FishingHook;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class NoPush extends Module {
    public NoPush() {
        super("NoPush", Category.MOVEMENT);
    }

    public Setting<Boolean> blocks = new Setting<>("Blocks", true);
    public Setting<Boolean> players = new Setting<>("Players", true);
    public Setting<Boolean> water = new Setting<>("Liquids", true);
    public Setting<Boolean> fishingHook = new Setting<>("FishingHook", true);

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundEntityEventPacket pac && pac.getEventId() == 31 && pac.getEntity(mc.level) instanceof FishingHook hook && fishingHook.getValue())
            if (hook.getHookedIn() == mc.player) e.cancel();
    }
}
