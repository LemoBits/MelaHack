package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerMoveC2SPacket;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AntiHunger extends Module {
    public AntiHunger() {
        super("AntiHunger", Category.PLAYER);
    }

    private final Setting<Boolean> ground = new Setting<>("CancelGround", true);
    private final Setting<Boolean> sprint = new Setting<>("CancelSprint", true);


    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof ServerboundMovePlayerPacket pac && ground.getValue())
            ((IPlayerMoveC2SPacket) pac).setOnGround(false);

        if (e.getPacket() instanceof ServerboundPlayerCommandPacket pac && sprint.getValue())
            if (pac.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING)
                e.cancel();
    }
}
