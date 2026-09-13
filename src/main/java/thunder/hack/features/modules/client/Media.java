package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IGameMessageS2CPacket;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public final class Media extends Module {
    public static final Setting<Boolean> skinProtect = new Setting<>("Skin Protect", true);
    public static final Setting<Boolean> nickProtect = new Setting<>("Nick Protect", true);

    public Media() {
        super("Media", Category.CLIENT);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof ClientboundSystemChatPacket pac && nickProtect.getValue()) {
            for (PlayerInfo ple : mc.player.connection.getOnlinePlayers()) {
                if (pac.content().getString().contains(ple.getProfile().getName())) {
                    IGameMessageS2CPacket packet = e.getPacket();
                    packet.setContent(Component.nullToEmpty(pac.content().getString().replace(ple.getProfile().getName(), "Protected")));
                }
            }
        }
    }
}
