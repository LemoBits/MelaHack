package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.item.Items;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.Objects;

public class MessageAppend extends Module {
    public MessageAppend() {
        super("MessageAppend", Category.MISC);
    }

    private final Setting<String> word = new Setting<>("word", " TH RECODE");
    private String skip;

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof ServerboundChatPacket pac) {
            if (Objects.equals(pac.message(), skip)) {
                return;
            }

            // Чтоб не добавляло когда ты вводишь капчу на сервере
            if (mc.player.getMainHandItem().getItem() == Items.FILLED_MAP || mc.player.getOffhandItem().getItem() == Items.FILLED_MAP)
                return;

            if (pac.message().startsWith("/") || pac.message().startsWith(Managers.COMMAND.getPrefix()))
                return;

            skip = pac.message() + word.getValue();
            mc.player.connection.sendChat(pac.message() + word.getValue());
            e.cancel();
        }
    }
}